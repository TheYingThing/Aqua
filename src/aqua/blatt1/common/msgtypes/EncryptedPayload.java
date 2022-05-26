package aqua.blatt1.common.msgtypes;

import java.io.Serializable;

@SuppressWarnings("serial")
public class EncryptedPayload implements Serializable {

    private final byte[] encryptedPayload;

    public EncryptedPayload(byte[] encryptedPayload) {
        this.encryptedPayload = encryptedPayload;
    }

    public byte[] getEncryptedPayload() {
        return encryptedPayload;
    }
}
