package aqua.blatt1.broker;

import aqua.blatt1.client.AquaClient;
import aqua.blatt1.common.SecureEndpoint;
import aqua.blatt1.common.msgtypes.*;
import static aqua.blatt1.common.Properties.*;
import aqua.blatt2.broker.PoisonPill;
import messaging.*;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.swing.JOptionPane;

public class Broker implements AquaBroker{
    private Endpoint endpoint = new SecureEndpoint(4711);
    private ClientCollection<AquaClient> collection = new ClientCollection<AquaClient>();
    volatile private boolean stopRequested = false;
    private int index = 1;
    ReadWriteLock lock = new ReentrantReadWriteLock();
    private Timer timer = new Timer();

    private final int LEASELENGTH = 5;

    @Override
    public void sendRegisterRequest(RegisterRequest registerRequest) throws RemoteException {

    }

    @Override
    public void sendDeregisterRequest(DeregisterRequest deregisterRequest) throws RemoteException {

    }

    @Override
    public void sendNameResolutionRequest(NameResolutionRequest nameResolutionRequest) throws RemoteException {

    }

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
                int i = collection.indexOf(((RegisterRequest) payload).getStub());
                try {
                    if (i != -1) {
                        register(collection.getId(i), new RegisterResponse(collection.getId(i), LEASELENGTH, true), ((RegisterRequest) payload).getStub());
                    } else {
                        register(clientId, new RegisterResponse(clientId, LEASELENGTH, false), ((RegisterRequest) payload).getStub());
                        index++;
                    }
                } catch (RemoteException e) {
                    throw new RuntimeException(e);
                }
            } else if (payload instanceof DeregisterRequest) {
                try {
                    String id = ((DeregisterRequest) payload).getId();
                    int index = collection.indexOf(id);
                    deregister(index);
                } catch (RemoteException e) {
                    throw new RuntimeException(e);
                }

            } else if (payload instanceof NameResolutionRequest) {
                AquaClient stub = collection.getClient(collection.indexOf(((NameResolutionRequest) payload).getTankID()));
                //InetSocketAddress address = collection.getClient(collection.indexOf(((NameResolutionRequest) payload).getTankID()));
                try {
                    stub.sendNameResolutionResponse(new NameResolutionResponse(stub, ((NameResolutionRequest) payload).getRequestID()));
                } catch (RemoteException e) {
                    throw new RuntimeException(e);
                }
                //endpoint.send(sender, new NameResolutionResponse(address, ((NameResolutionRequest) payload).getRequestID()));
            }

            TimerTask task = new TimerTask() {
                @Override
                public void run() {
                    try {
                        for(int i = 0 ; i < collection.size() ; i++) {
                            Long diff = ChronoUnit.MILLIS.between(collection.getRegTime(i), Instant.now());
                            if(diff.compareTo(5000L) > 0) {
                                deregister(i);
                            }
                        }
                    } catch (RemoteException e) {
                        throw new RuntimeException(e);
                    }


                }
            };

            timer.schedule(task, 0, 1000);
        }

        public void register(String clientId, RegisterResponse response, AquaClient stub) throws RemoteException {
            Instant d = Instant.now();
            lock.readLock().lock();
            int i = collection.indexOf(clientId);
            lock.readLock().unlock();
            lock.writeLock().lock();
            if(i == -1) {
                collection.add(clientId, stub);
            } else {
                collection.setRegTime(i, d);
            }
            lock.writeLock().unlock();

            lock.readLock().lock();
            AquaClient leftStub = collection.getLeftNeighorOf(collection.indexOf(clientId));
            AquaClient rightStub = collection.getRightNeighorOf(collection.indexOf(clientId));
            lock.readLock().unlock();
            NeighborUpdate clientNeighbors = new NeighborUpdate(leftStub, rightStub);
            NeighborUpdate leftNeighbors = new NeighborUpdate(null, stub);
            NeighborUpdate rightNeighbors = new NeighborUpdate(stub, null);

            stub.sendNeighborUpdate(clientNeighbors);
            rightStub.sendNeighborUpdate(rightNeighbors);
            leftStub.sendNeighborUpdate(leftNeighbors);
            //endpoint.send(sender, clientNeighbors);
            //endpoint.send(rightStub, rightNeighbors);
            //endpoint.send(leftStub, leftNeighbors);

            if (index == 1) {
                NeighborUpdate firstNeighbors = new NeighborUpdate(stub, stub);
                Token token = new Token();
                stub.sendNeighborUpdate(firstNeighbors);
                stub.sendToken();
                //endpoint.send(sender, firstNeighbors);
                //endpoint.send(sender, token);
            }
            stub.sendRegisterResponse(response);
            //endpoint.send(sender, response);
        }

        public void deregister(int i) throws RemoteException {
            lock.readLock().lock();
            AquaClient leftStub = collection.getLeftNeighorOf(i);
            AquaClient rightStub = collection.getRightNeighorOf(i);
            lock.readLock().unlock();
            NeighborUpdate leftNeighbors = new NeighborUpdate(null, rightStub);
            NeighborUpdate rightNeighbors = new NeighborUpdate(leftStub, null);

            lock.writeLock().lock();
            collection.remove(i);
            lock.writeLock().unlock();

            leftStub.sendNeighborUpdate(leftNeighbors);
            rightStub.sendNeighborUpdate(rightNeighbors);
            //endpoint.send(leftAddress, leftNeighbors);
            //endpoint.send(rightAddress, rightNeighbors);
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


    public static void main(String[] args) throws RemoteException {
        AquaBroker stub = (AquaBroker) UnicastRemoteObject.exportObject(new Broker(), 0);
        Registry registry = LocateRegistry.createRegistry(Registry.REGISTRY_PORT);
        registry.rebind(BROKER_NAME, stub);
        Broker b = new Broker();
        b.broker();
    }

}