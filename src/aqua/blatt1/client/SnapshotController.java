package aqua.blatt1.client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.rmi.RemoteException;

public class SnapshotController implements ActionListener {
	private final Component parent;
	private final TankModel tank;

	public SnapshotController(Component parent, TankModel tank ) {
		this.parent = parent;
		this.tank = tank;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		try {
			tank.initiateSnapshot();
		} catch (RemoteException ex) {
			throw new RuntimeException(ex);
		}
	}
}
