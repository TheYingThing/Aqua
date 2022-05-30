package aqua.blatt1.common.msgtypes;

import aqua.blatt1.client.AquaClient;

import java.io.Serializable;
import java.net.InetSocketAddress;

@SuppressWarnings("serial")
public final class NameResolutionResponse implements Serializable {
    private final AquaClient tankStub;
    private final String requestID;

    public NameResolutionResponse(AquaClient tankStub, String requestID) {
        this.requestID = requestID;
        this.tankStub = tankStub;
    }

    public String getRequestID() {
        return requestID;
    }

    public AquaClient getTankAddress() {
        return tankStub;
    }
}