package aqua.blatt1.broker;

import aqua.blatt1.common.msgtypes.EncryptedPayload;
import messaging.Endpoint;
import messaging.Message;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.InetSocketAddress;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class SecureEndpoint extends Endpoint {

    private final String MATERIAL = "CAFEBABECAFEBABE";
    private final String ALGORITHM = "AES";
    Cipher encryptCipher;
    Cipher decryptCipher;


    public SecureEndpoint(int port) {
        super(port);
        setUpCiphers();
    }

    public SecureEndpoint() {
        super();
        setUpCiphers();
    }

    private void setUpCiphers() {
        try {
            SecretKeySpec keySpec = new SecretKeySpec(MATERIAL.getBytes(), ALGORITHM);
            encryptCipher = Cipher.getInstance(ALGORITHM);
            decryptCipher = Cipher.getInstance(ALGORITHM);
            encryptCipher.init(Cipher.ENCRYPT_MODE, keySpec);
            decryptCipher.init(Cipher.DECRYPT_MODE, keySpec);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException e) {
            throw new RuntimeException(e);
        }
    }

    public void send(InetSocketAddress receiver, Serializable payload) {
        try {
            super.send(receiver, encrypt(payload));
        } catch (IllegalBlockSizeException | IOException | InvalidKeyException e) {
            throw new RuntimeException(e);
        }
    }

    public Message blockingReceive() {
        Message message = super.blockingReceive();
        return new Message(decrypt(message.getPayload()), message.getSender());
    }

    public Message nonBlockingReceive() {
        Message message = super.nonBlockingReceive();
        return new Message(decrypt(message.getPayload()), message.getSender());
    }

    private Serializable encrypt(Serializable payload) throws IllegalBlockSizeException, IOException, InvalidKeyException {
        byte[] encryptedBytes = {};
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
            objectOutputStream.writeObject(payload);
            objectOutputStream.flush();

            byte[] byteArray = byteArrayOutputStream.toByteArray();
            byteArrayOutputStream.close();

            encryptedBytes = encryptCipher.doFinal(byteArray);
            return new EncryptedPayload(encryptedBytes);

        } catch (IllegalBlockSizeException | IOException | BadPaddingException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private Serializable decrypt(Serializable encryptedPayload) {
        byte[] encryptedBytes = ((EncryptedPayload) encryptedPayload).getEncryptedPayload();
        ObjectInput objectInput;
        Serializable decryptedPayload;

        try {
            byte[] decryptedBytes = decryptCipher.doFinal(encryptedBytes);
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(decryptedBytes);
            objectInput = new ObjectInputStream(byteArrayInputStream);
            decryptedPayload = (Serializable) objectInput.readObject();
            return decryptedPayload;

        } catch (IOException | ClassNotFoundException | BadPaddingException |
                 IllegalBlockSizeException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}
