package aqua.blatt1.broker;

import aqua.blatt1.common.*;
import aqua.blatt1.common.msgtypes.*;
import aqua.blatt2.broker.PoisonPill;
import messaging.*;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
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
    ReadWriteLock lock = new ReentrantReadWriteLock();
    private Timer timer = new Timer();

    private final int LEASELENGTH = 5;

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
                int i = collection.indexOf(sender);
                if(i != -1) {
                    register(collection.getId(i), new RegisterResponse(collection.getId(i), LEASELENGTH, true));
                } else {
                    register(clientId, new RegisterResponse(clientId, LEASELENGTH, false));
                    index++;
                }

            } else if (payload instanceof DeregisterRequest) {
                String id = ((DeregisterRequest) payload).getId();
                int index = collection.indexOf(id);
                deregister(index);
            } else if (payload instanceof NameResolutionRequest) {
                InetSocketAddress address = collection.getClient(collection.indexOf(((NameResolutionRequest) payload).getTankID()));
                endpoint.send(sender, new NameResolutionResponse(address, ((NameResolutionRequest) payload).getRequestID()));
            }

            TimerTask task = new TimerTask() {
                @Override
                public void run() {
                    for(int i = 0 ; i < collection.size() ; i++) {
                        Long diff = ChronoUnit.MILLIS.between(collection.getRegTime(i), Instant.now());
                        if(diff.compareTo(5000L) > 0) {
                            deregister(i);
                        }
                    }

                }
            };

            timer.schedule(task, 0, 1000);
        }

        public void register(String clientId, RegisterResponse response) {
            Instant d = Instant.now();
            lock.readLock().lock();
            int i = collection.indexOf(clientId);
            lock.readLock().unlock();
            lock.writeLock().lock();
            if(i == -1) {
                collection.add(clientId, sender);
            } else {
                collection.setRegTime(i, d);
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

        public void deregister(int i) {
            lock.readLock().lock();
            InetSocketAddress leftAddress = collection.getLeftNeighorOf(i);
            InetSocketAddress rightAddress = collection.getRightNeighorOf(i);
            lock.readLock().unlock();
            NeighborUpdate leftNeighbors = new NeighborUpdate(null, rightAddress);
            NeighborUpdate rightNeighbors = new NeighborUpdate(leftAddress, null);

            lock.writeLock().lock();
            collection.remove(i);
            lock.writeLock().unlock();

            endpoint.send(leftAddress, leftNeighbors);
            endpoint.send(rightAddress, rightNeighbors);
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