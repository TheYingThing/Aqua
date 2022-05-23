package aqua.blatt1.common;

import java.io.Serializable;

public class CipherText implements Serializable {
    private final byte[] cipherText;

    CipherText(byte[] payload) {
        this.cipherText = payload;
    }

    public byte[] getCipherText() {
        return this.cipherText;
    }
}
