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
		//ClientCommunicator communicator = new ClientCommunicator();
		//System.setProperty("java.rmi.server.hostname", BROKER_NAME);

		Registry registry = LocateRegistry.getRegistry(Registry.REGISTRY_PORT);

		AquaBroker broker = (AquaBroker) registry.lookup(BROKER_NAME);

		TankModel tankModel = new TankModel(broker);

		SwingUtilities.invokeLater(new AquaGui(tankModel));

		tankModel.run();
	}
}
