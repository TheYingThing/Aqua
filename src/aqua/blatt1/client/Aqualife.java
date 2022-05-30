package aqua.blatt1.client;

import aqua.blatt1.broker.AquaBroker;
import aqua.blatt1.broker.Broker;

import javax.swing.SwingUtilities;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import static aqua.blatt1.common.Properties.BROKER_NAME;

public class Aqualife {

	public static void main(String[] args) throws NotBoundException, RemoteException {
		ClientCommunicator communicator = new ClientCommunicator();

		Registry registry = LocateRegistry.getRegistry(Registry.REGISTRY_PORT);

		AquaBroker broker = (AquaBroker) registry.lookup(BROKER_NAME);

		TankModel tankModel = new TankModel(communicator.newClientForwarder(), broker);
		AquaClient stub = (AquaClient) UnicastRemoteObject.exportObject(tankModel, 0);
		tankModel.setStub(stub);

		communicator.newClientReceiver(tankModel).start();

		SwingUtilities.invokeLater(new AquaGui(tankModel));

		tankModel.run();
	}
}
