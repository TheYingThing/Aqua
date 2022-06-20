package aqua.blatt1.broker;

import aqua.blatt1.common.msgtypes.DeregisterRequest;
import aqua.blatt1.common.msgtypes.NameResolutionRequest;
import aqua.blatt1.common.msgtypes.RegisterRequest;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface AquaBroker extends Remote {
    void handleRegisterRequest(RegisterRequest registerRequest) throws RemoteException;
    void handleDeregisterRequest(DeregisterRequest deregisterRequest) throws RemoteException;
    void handleNameResolutionRequest(NameResolutionRequest nameResolutionRequest) throws RemoteException;
}
