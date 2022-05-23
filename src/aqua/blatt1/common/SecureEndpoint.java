package aqua.blatt1.common;

import messaging.Endpoint;
import messaging.Message;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class SecureEndpoint extends Endpoint {
    private static final String MATERIAL = "CAFEBABECAFEBABE";
    private static final String ENCODING_KEY = "AES";
    private Cipher encodingCipher;
    private Cipher decodingCipher;

    public SecureEndpoint() {
        super();
        initCiphers();
    }

    public SecureEndpoint(int port) {
        super(port);
        initCiphers();
    }

    private void initCiphers() {
        try {
            SecretKeySpec key = new SecretKeySpec(MATERIAL.getBytes(), ENCODING_KEY);
            encodingCipher = Cipher.getInstance(ENCODING_KEY);
            encodingCipher.init(Cipher.ENCRYPT_MODE, key);
            decodingCipher = Cipher.getInstance(ENCODING_KEY);
            decodingCipher.init(Cipher.DECRYPT_MODE, key);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (NoSuchPaddingException e) {
            throw new RuntimeException(e);
        } catch (InvalidKeyException e) {
            throw new RuntimeException(e);
        }
    }

    public Message decryptPayload(Message msg) {
        try {
            CipherText payloadCipher = (CipherText) msg.getPayload();
            byte[] decodedPayload = decodingCipher.doFinal(payloadCipher.getCipherText());
            ByteArrayInputStream bos = new ByteArrayInputStream(decodedPayload);
            ObjectInputStream ois = new ObjectInputStream(bos);
            return new Message((Serializable) ois.readObject(), msg.getSender());
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (IllegalBlockSizeException e) {
            throw new RuntimeException(e);
        } catch (BadPaddingException e) {
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public Serializable encryptPayload(Serializable payload) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(payload);
            oos.flush();
            byte[] serializedPayload = bos.toByteArray();
            byte[] encodedPayload = encodingCipher.doFinal(serializedPayload);
            return new CipherText(encodedPayload);
        } catch (IllegalBlockSizeException e) {
            throw new RuntimeException(e);
        } catch (BadPaddingException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public void send(InetSocketAddress receiver, Serializable payload) {
        super.send(receiver, encryptPayload(payload));
    }
    public Message blockingReceive() {
        return decryptPayload(super.blockingReceive());
    }

    public Message nonBlockingReceive() {
        return decryptPayload(super.nonBlockingReceive());
    }
}