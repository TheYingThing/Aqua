package aqua.blatt1.common.msgtypes;

import java.io.Serializable;
import java.net.InetSocketAddress;

@SuppressWarnings("serial")
public class LocationUpdate implements Serializable {
    private final String fishId;

    public LocationUpdate(String fishId) {
        this.fishId = fishId;
    }

    public String getFishId() {
        return fishId;
    }
}
