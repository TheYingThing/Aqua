package aqua.blatt1.client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ToggleController implements ActionListener {
	private final Component parent;
	private final TankModel tank;
	private final String fishID;

	public ToggleController(Component parent, TankModel tank, String fishID ) {
		this.parent = parent;
		this.tank = tank;
		this.fishID = fishID;
	}
	@Override
	public void actionPerformed(ActionEvent e) {
		tank.locateFishGlobally(fishID);

	}
}
