package aqua.blatt1.broker;

import aqua.blatt1.common.*;
import aqua.blatt1.common.msgtypes.*;
import aqua.blatt2.broker.PoisonPill;
import messaging.*;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.swing.JOptionPane;

public class Broker {
    private Endpoint endpoint = new Endpoint(4711);
    private ClientCollection<InetSocketAddress> collection = new ClientCollection<InetSocketAddress>();
    volatile private boolean stopRequested = false;
    int index = 1;
    ReadWriteLock lock = new ReentrantReadWriteLock();

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
            String clientId = "tank" + index;

            if (payload instanceof RegisterRequest) {
                register(clientId);
            } else if (payload instanceof DeregisterRequest) {
                deregister();
            }
        }

        public void register(String clientId) {
            lock.writeLock().lock();
            collection.add(clientId, sender);
            lock.writeLock().unlock();

            RegisterResponse response = new RegisterResponse(clientId);
            lock.readLock().lock();
            InetSocketAddress leftAddress = collection.getLeftNeighorOf(collection.indexOf(clientId));
            lock.readLock().lock();
            lock.readLock().lock();
            InetSocketAddress rightAddress = collection.getRightNeighorOf(collection.indexOf(clientId));
            lock.readLock().lock();
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
            index++;
        }

        public void deregister() {
            String id = ((DeregisterRequest) payload).getId();
            lock.readLock().lock();
            InetSocketAddress leftAddress = collection.getLeftNeighorOf(collection.indexOf(id));
            InetSocketAddress rightAddress = collection.getRightNeighorOf(collection.indexOf(id));
            lock.readLock().lock();
            NeighborUpdate leftNeighbors = new NeighborUpdate(null, rightAddress);
            NeighborUpdate rightNeighbors = new NeighborUpdate(leftAddress, null);

            lock.writeLock().lock();
            collection.remove(collection.indexOf(id));
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