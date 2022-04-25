package aqua.blatt1.common.msgtypes;

import java.io.Serializable;
import java.net.InetSocketAddress;

@SuppressWarnings("serial")
public final class NameResolutionRequest implements Serializable {
    private final String tankID;
    private final String requestID;

    public NameResolutionRequest(String tankID, String requestID) {
        this.requestID = requestID;
        this.tankID = tankID;
    }

}