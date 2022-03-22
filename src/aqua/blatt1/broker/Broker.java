package aqua.blatt1.broker;

import aqua.blatt1.client.ClientCommunicator;
import aqua.blatt1.common.Direction;
import aqua.blatt1.common.FishModel;
import aqua.blatt1.common.msgtypes.DeregisterRequest;
import aqua.blatt1.common.msgtypes.HandoffRequest;
import aqua.blatt1.common.msgtypes.RegisterRequest;
import aqua.blatt1.common.msgtypes.RegisterResponse;
import messaging.Endpoint;
import messaging.Message;

import java.io.Serializable;
import java.net.InetSocketAddress;

public class Broker {
    private Endpoint endpoint = new Endpoint(4711);
    private ClientCollection<InetSocketAddress> collection = new ClientCollection<InetSocketAddress>();

    public void broker() {
        int index = 1;
        while(true) {
            Message message = endpoint.blockingReceive();
            Serializable payload = message.getPayload();
            InetSocketAddress sender = message.getSender();
            String clientId = "tank" + index;

            if (payload instanceof RegisterRequest) {
                collection.add(clientId, sender);
                RegisterResponse response = new RegisterResponse(clientId);
                endpoint.send(sender, response);
                index++;

            } else if (payload instanceof DeregisterRequest) {
                String id = ((DeregisterRequest) payload).getId();
                collection.remove(collection.indexOf(id));

            } else if (payload instanceof HandoffRequest) {
                FishModel fish = ((HandoffRequest) payload).getFish();
                int tankIndex = collection.indexOf(sender);

                if (fish.getDirection().equals(Direction.LEFT)) {
                    InetSocketAddress leftTank = collection.getLeftNeighorOf(tankIndex);
                    endpoint.send(leftTank, payload);

                } else if (fish.getDirection().equals(Direction.RIGHT)) {
                    InetSocketAddress rightTank = collection.getRightNeighorOf(tankIndex);
                    endpoint.send(rightTank, payload);
                }
            }
        }
    }

    public static void main(String[] args) {
        Broker b = new Broker();
        b.broker();
    }
}
