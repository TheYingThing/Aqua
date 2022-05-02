package aqua.blatt1.common.msgtypes;

import java.io.Serializable;
import java.net.InetSocketAddress;

@SuppressWarnings("serial")
public class NameResolutionResponse implements Serializable {

    private final InetSocketAddress tankAddress;
    private final String requestID;

    public NameResolutionResponse(InetSocketAddress tankAddress, String requestID) {
        this.tankAddress = tankAddress;
        this.requestID = requestID;
    }

    public String getRequestID() {
        return requestID;
    }

    public InetSocketAddress getTankAddress() {
        return tankAddress;
    }
}
