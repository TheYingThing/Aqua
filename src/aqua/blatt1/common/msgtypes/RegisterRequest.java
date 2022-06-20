package aqua.blatt1.common.msgtypes;

import aqua.blatt1.client.AquaClient;

import java.io.Serializable;

@SuppressWarnings("serial")
public final class RegisterRequest implements Serializable {
    private final AquaClient stub;
    public RegisterRequest(AquaClient stub) {
        this.stub = stub;
    }
    public AquaClient getStub() {
        return stub;
    }
}