package aqua.blatt1.common.msgtypes;

import java.io.Serializable;
import java.security.PublicKey;

@SuppressWarnings("serial")
public final class KeyXChangeRequest implements Serializable {
    private final PublicKey publicKey;

    public KeyXChangeRequest(PublicKey publicKey) {
        this.publicKey = publicKey;
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }
}
