package aqua.blatt1.common.msgtypes;

import aqua.blatt1.client.AquaClient;

import java.io.Serializable;
import java.net.InetSocketAddress;

@SuppressWarnings("serial")
public final class LocationUpdate implements Serializable {
    private final String fishID;
    private final AquaClient sender;

    public LocationUpdate(String fishID, AquaClient sender) {
        this.fishID = fishID;
        this.sender = sender;
    }

    public String getFishID() {
        return fishID;
    }

    public AquaClient getSender() {
        return sender;
    }
}
