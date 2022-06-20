package aqua.blatt1.client;

import aqua.blatt1.common.msgtypes.*;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface AquaClient extends Remote {

    void handleRegisterResponse(RegisterResponse registerResponse) throws RemoteException;
    void handleHandoffRequest(HandoffRequest handoffRequest) throws RemoteException;
    void handleNeighborUpdate(NeighborUpdate neighborUpdate) throws RemoteException;
    void handleToken() throws RemoteException;
    void handleSnapshotMarker(SnapshotMarker snapshotMarker) throws RemoteException;
    void handleCollectionToken(CollectionToken collectionToken) throws RemoteException;
    void handleLocationRequest(LocationRequest locationRequest) throws RemoteException;
    void handleNameResolutionResponse(NameResolutionResponse nameResolutionResponse) throws RemoteException;
    void handleLocationUpdate(LocationUpdate locationUpdate) throws RemoteException;
}
