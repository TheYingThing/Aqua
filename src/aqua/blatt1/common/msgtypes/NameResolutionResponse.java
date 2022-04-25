package aqua.blatt1.common.msgtypes;

import java.io.Serializable;
import java.net.InetSocketAddress;

@SuppressWarnings("serial")
public final class NameResolutionResponse implements Serializable {
    private final InetSocketAddress tankAddress;
    private final String requestID;

    public NameResolutionResponse(InetSocketAddress tankAddress, String requestID) {
        this.requestID = requestID;
        this.tankAddress = tankAddress;
    }

    public String getRequestID() {
        return requestID;
    }

    public InetSocketAddress getTankAddress() {
        return tankAddress;
    }
}