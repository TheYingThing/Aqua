package aqua.blatt1.client;

import java.net.InetSocketAddress;

import aqua.blatt1.common.msgtypes.*;
import messaging.Endpoint;
import messaging.Message;
import aqua.blatt1.common.FishModel;
import aqua.blatt1.common.Properties;

public class ClientCommunicator {
	private final Endpoint endpoint;

	public ClientCommunicator() {
		endpoint = new Endpoint();
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

		public void sendToken(InetSocketAddress tank) { endpoint.send(tank, new Token()); }

		public void sendMarker(InetSocketAddress tank) { endpoint.send(tank, new SnapshotMarker());}

		public void sendCollectionToken(InetSocketAddress tank, CollectionToken token) { endpoint.send(tank,token);}
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
					tankModel.onRegistration(((RegisterResponse) msg.getPayload()).getId());

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
