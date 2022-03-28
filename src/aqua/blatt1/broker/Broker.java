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
                lock.writeLock().lock();
                collection.add(clientId, sender);
                lock.writeLock().unlock();
                RegisterResponse response = new RegisterResponse(clientId);
                endpoint.send(sender, response);
                index++;

            } else if (payload instanceof DeregisterRequest) {
                String id = ((DeregisterRequest) payload).getId();
                lock.writeLock().lock();
                collection.remove(collection.indexOf(id));
                lock.writeLock().unlock();

            } else if (payload instanceof HandoffRequest) {
                FishModel fish = ((HandoffRequest) payload).getFish();

                int tankIndex = collection.indexOf(sender);

                if (fish.getDirection().equals(Direction.LEFT)) {
                    lock.writeLock().lock();
                    InetSocketAddress leftTank = collection.getLeftNeighorOf(tankIndex);
                    lock.writeLock().unlock();
                    endpoint.send(leftTank, payload);

                } else if (fish.getDirection().equals(Direction.RIGHT)) {
                    lock.writeLock().lock();
                    InetSocketAddress rightTank = collection.getRightNeighorOf(tankIndex);
                    lock.writeLock().unlock();
                    endpoint.send(rightTank, payload);
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
