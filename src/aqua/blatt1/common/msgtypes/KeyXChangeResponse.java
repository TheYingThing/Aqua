package aqua.blatt1.common.msgtypes;

import java.io.Serializable;
import java.security.PublicKey;
@SuppressWarnings("serial")
public final class KeyXChangeResponse implements Serializable {
    private final PublicKey publicKey;

    public KeyXChangeResponse(PublicKey publicKey) {
        this.publicKey = publicKey;
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }
}
