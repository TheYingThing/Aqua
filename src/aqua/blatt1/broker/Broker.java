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
    private final ClientCollection<AquaClient> collection = new ClientCollection<AquaClient>();
    volatile private boolean stopRequested = false;
    private int index = 1;
    ReadWriteLock lock = new ReentrantReadWriteLock();
    private final Timer timer = new Timer();

    private final int LEASELENGTH = 5;

    @Override
    public void handleRegisterRequest(RegisterRequest registerRequest) throws RemoteException {
        String clientId = "tank" + index;
        int i = collection.indexOf((registerRequest).getStub());
        try {
            if (i != -1) {
                register(collection.getId(i), new RegisterResponse(collection.getId(i), LEASELENGTH, true), registerRequest.getStub());
            } else {
                register(clientId, new RegisterResponse(clientId, LEASELENGTH, false), registerRequest.getStub());
                index++;
            }
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void handleDeregisterRequest(DeregisterRequest deregisterRequest) throws RemoteException {
        try {
            String id = deregisterRequest.getId();
            int index = collection.indexOf(id);
            deregister(index);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void handleNameResolutionRequest(NameResolutionRequest nameResolutionRequest) throws RemoteException {
        AquaClient stub = collection.getClient(collection.indexOf(nameResolutionRequest.getTankID()));
        try {
            stub.handleNameResolutionResponse(new NameResolutionResponse(stub, nameResolutionRequest.getRequestID()));
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    private class StopRequestTask implements Runnable {
        @Override
        public void run() {
            JOptionPane.showMessageDialog(null, "Pres OK button to stop server");
            stopRequested = true;
        }
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

        stub.handleNeighborUpdate(clientNeighbors);
        rightStub.handleNeighborUpdate(rightNeighbors);
        leftStub.handleNeighborUpdate(leftNeighbors);

        if (index == 1) {
            NeighborUpdate firstNeighbors = new NeighborUpdate(stub, stub);
            Token token = new Token();
            stub.handleNeighborUpdate(firstNeighbors);
            stub.handleToken();
        }
        stub.handleRegisterResponse(response);
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

        leftStub.handleNeighborUpdate(leftNeighbors);
        rightStub.handleNeighborUpdate(rightNeighbors);
    }

    /*public void broker() {
        ExecutorService executor = Executors.newFixedThreadPool(4);
        Runnable stopService = new StopRequestTask();
        executor.execute(stopService);

        while(!stopRequested) {
        }
        executor.shutdown();
    }*/

    public static void main(String[] args) throws RemoteException {
        Broker b = new Broker();
        AquaBroker stub = (AquaBroker) UnicastRemoteObject.exportObject(b, 0);
        Registry registry = LocateRegistry.createRegistry(Registry.REGISTRY_PORT);
        registry.rebind(BROKER_NAME, stub);
    }

}