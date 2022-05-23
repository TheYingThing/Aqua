package aqua.blatt1.broker;

import aqua.blatt1.common.msgtypes.*;
import aqua.blatt2.broker.PoisonPill;
import messaging.*;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.swing.JOptionPane;

public class Broker {
    private Endpoint endpoint = new Endpoint(4711);
    private ClientCollection<InetSocketAddress> collection = new ClientCollection<InetSocketAddress>();
    volatile private boolean stopRequested = false;
    private int index = 1;
    private int leaseTime = 5000;
    private int checkLease = 2000;
    ReadWriteLock lock = new ReentrantReadWriteLock();
    protected volatile Timer timer = new Timer();


    private class StopRequestTask implements Runnable {
        @Override
        public void run() {
            JOptionPane.showMessageDialog(null, "Pres OK button to stop server");
            stopRequested = true;
        }
    }

    private class BrokerTask implements Runnable {
        Serializable payload;
        InetSocketAddress sender;

        public BrokerTask(Message message) {
            this.payload = message.getPayload();
            this.sender = message.getSender();
        }

        @Override
        public void run() {
            if (payload instanceof RegisterRequest) {
                String clientId = "tank" + index;
                lock.readLock().lock();
                int clientIndex = collection.indexOf(sender);
                lock.readLock().unlock();

                if(clientIndex == -1) {
                    register(clientId, new RegisterResponse(clientId, leaseTime, true));
                    index++;
                } else {
                    register(collection.getClientId(clientIndex), new RegisterResponse(collection.getClientId(clientIndex), leaseTime, false));
                }

            } else if (payload instanceof DeregisterRequest) {
                String id = ((DeregisterRequest) payload).getId();
                lock.readLock().lock();
                int clientIndex = collection.indexOf(id);
                lock.readLock().unlock();
                deregister(clientIndex);
            } else if (payload instanceof NameResolutionRequest) {
                String tankId = ((NameResolutionRequest) payload).getTankID();
                String requestId = ((NameResolutionRequest) payload).getRequestID();
                lock.readLock().lock();
                int index = collection.indexOf(tankId);
                InetSocketAddress tankaddress = collection.getClient(index);
                lock.readLock().lock();

                NameResolutionResponse nameResolutionResponse = new NameResolutionResponse(tankaddress, requestId);
                endpoint.send(sender, nameResolutionResponse);
            }

            TimerTask task = new TimerTask() {
                @Override
                public void run() {
                    checkClientCollection();
                }
            };
            timer.schedule(task, checkLease);
        }

        public void register(String clientId, RegisterResponse response) {
            lock.readLock().lock();
            int clientIndex = collection.indexOf(clientId);
            lock.readLock().unlock();
            Instant timestamp = Instant.now();

            lock.writeLock().lock();
            if(clientIndex == -1) {
                collection.add(clientId, sender, timestamp);
            } else {
                collection.replaceTimestamp(clientIndex, timestamp);
            }
            lock.writeLock().unlock();

            lock.readLock().lock();
            InetSocketAddress leftAddress = collection.getLeftNeighorOf(collection.indexOf(clientId));
            InetSocketAddress rightAddress = collection.getRightNeighorOf(collection.indexOf(clientId));
            lock.readLock().unlock();
            NeighborUpdate clientNeighbors = new NeighborUpdate(leftAddress, rightAddress);
            NeighborUpdate leftNeighbors = new NeighborUpdate(null, sender);
            NeighborUpdate rightNeighbors = new NeighborUpdate(sender, null);

            endpoint.send(sender, clientNeighbors);
            endpoint.send(rightAddress, rightNeighbors);
            endpoint.send(leftAddress, leftNeighbors);

            if (index == 1) {
                NeighborUpdate firstNeighbors = new NeighborUpdate(sender, sender);
                Token token = new Token();
                endpoint.send(sender, firstNeighbors);
                endpoint.send(sender, token);
            }
            endpoint.send(sender, response);
        }

        public void deregister(int clientIndex) {
            lock.readLock().lock();
            InetSocketAddress leftAddress = collection.getLeftNeighorOf(clientIndex);
            InetSocketAddress rightAddress = collection.getRightNeighorOf(clientIndex);
            lock.readLock().unlock();
            NeighborUpdate leftNeighbors = new NeighborUpdate(null, rightAddress);
            NeighborUpdate rightNeighbors = new NeighborUpdate(leftAddress, null);

            lock.writeLock().lock();
            collection.remove(clientIndex);
            lock.writeLock().unlock();

            endpoint.send(leftAddress, leftNeighbors);
            endpoint.send(rightAddress, rightNeighbors);
        }

        public void checkClientCollection() {
            Instant now = Instant.now();
            lock.readLock().lock();
            int size = collection.size();
            lock.readLock().unlock();

            for(int i = 0; i < size; i++) {
                lock.readLock().lock();
                Instant clientTimestamp = collection.getTimestamp(i);
                lock.readLock().unlock();

                if (now.minus(leaseTime, ChronoUnit.MILLIS).isAfter(clientTimestamp)) {
                    deregister(i);
                }
            }
        }
    }
    public void broker() {
        ExecutorService executor = Executors.newFixedThreadPool(4);
        Runnable stopService = new StopRequestTask();
        executor.execute(stopService);

        while(!stopRequested) {
            Message message = endpoint.blockingReceive();
            if (message.getPayload() instanceof PoisonPill) {
                break;
            }
            Runnable brokerTask = new BrokerTask(message);
            executor.execute(brokerTask);
        }
        executor.shutdown();
    }

    public static void main(String[] args) {
        Broker b = new Broker();
        b.broker();
    }
}