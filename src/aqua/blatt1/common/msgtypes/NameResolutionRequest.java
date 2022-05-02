package aqua.blatt1.common.msgtypes;

import java.io.Serializable;

@SuppressWarnings("serial")
public class NameResolutionRequest implements Serializable {

    private final String tankID;
    private final String requestID;

    public NameResolutionRequest(String tankID, String requestID) {
        this.tankID = tankID;
        this.requestID = requestID;
    }

    public String getTankID() {
        return tankID;
    }

    public String getRequestID() {
        return requestID;
    }
}
