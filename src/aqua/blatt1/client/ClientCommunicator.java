package aqua.blatt1.client;

import java.net.InetSocketAddress;

import aqua.blatt1.broker.SecureEndpoint;
import aqua.blatt1.common.msgtypes.*;
import messaging.Endpoint;
import messaging.Message;
import aqua.blatt1.common.FishModel;
import aqua.blatt1.common.Properties;

public class ClientCommunicator {
    private final SecureEndpoint endpoint;

    public ClientCommunicator() {
        endpoint = new SecureEndpoint();
    }

    public class ClientForwarder {
        private final InetSocketAddress broker;

        private ClientForwarder() {
            this.broker = new InetSocketAddress(Properties.HOST, Properties.PORT);
        }

        public void register() {
            endpoint.send(broker, new RegisterRequest());
        }

        public void deregister(String id) {
            endpoint.send(broker, new DeregisterRequest(id));
        }

        public void handOff(FishModel fish, InetSocketAddress tank) {
            endpoint.send(tank, new HandoffRequest(fish));
        }

        public void sendToken(InetSocketAddress tank) {
            endpoint.send(tank, new Token());
        }

        public void sendMarker(InetSocketAddress tank) {
            endpoint.send(tank, new SnapshotMarker());
        }

        public void sendCollectionToken(InetSocketAddress tank, CollectionToken token) {
            endpoint.send(tank, token);
        }

        public void sendLocationRequest(InetSocketAddress tank, String fishId) {
            endpoint.send(tank, new LocationRequest(fishId));
        }

        public void sendNameResolutionRequest(String tankId, String requestId) {
            endpoint.send(broker, new NameResolutionRequest(tankId, requestId));
        }

        public void sendLocationUpdate(InetSocketAddress tank, String fishId) {
            endpoint.send(tank, new LocationUpdate(fishId));
        }
    }

    public class ClientReceiver extends Thread {
        private final TankModel tankModel;

        private ClientReceiver(TankModel tankModel) {
            this.tankModel = tankModel;
        }

        @Override
        public void run() {
            while (!isInterrupted()) {
                Message msg = endpoint.blockingReceive();

                if (msg.getPayload() instanceof RegisterResponse)
                    tankModel.onRegistration(((RegisterResponse) msg.getPayload()).getId(), ((RegisterResponse) msg.getPayload()).getLeaseTime(), ((RegisterResponse) msg.getPayload()).isNewTank());

                if (msg.getPayload() instanceof HandoffRequest)
                    tankModel.receiveFish(((HandoffRequest) msg.getPayload()).getFish());

                if (msg.getPayload() instanceof NeighborUpdate)
                    tankModel.updateNeighbors(((NeighborUpdate) msg.getPayload()).getLeft(), ((NeighborUpdate) msg.getPayload()).getRight());

                if (msg.getPayload() instanceof Token)
                    tankModel.receiveToken();

                if (msg.getPayload() instanceof SnapshotMarker)
                    tankModel.receiveMarker(msg.getSender());

                if (msg.getPayload() instanceof CollectionToken)
                    tankModel.receiveCollectionToken((CollectionToken) msg.getPayload());

                if (msg.getPayload() instanceof LocationRequest)
                    tankModel.locateFishLocally(((LocationRequest) msg.getPayload()).getFishId());

                if (msg.getPayload() instanceof NameResolutionResponse)
                    tankModel.receiveNameResolutionResponse((NameResolutionResponse) msg.getPayload(), msg.getSender());

                if (msg.getPayload() instanceof LocationUpdate)
                    tankModel.receiveLocationUpdate(((LocationUpdate) msg.getPayload()).getFishId(), msg.getSender());
            }
            System.out.println("Receiver stopped.");
        }
    }

    public ClientForwarder newClientForwarder() {
        return new ClientForwarder();
    }

    public ClientReceiver newClientReceiver(TankModel tankModel) {
        return new ClientReceiver(tankModel);
    }

}
