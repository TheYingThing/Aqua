package aqua.blatt1.client;

import aqua.blatt1.common.FishModel;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ToggleController implements ActionListener {
    private final Component parent;
    private final String fish;
    private final TankModel tank;

    public ToggleController(Component parent, String fish, TankModel tank) {
        this.parent = parent;
        this.fish = fish;
        this.tank = tank;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        tank.locateFishGlobally(fish);
    }
}
