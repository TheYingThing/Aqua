package aqua.blatt1.common;

import messaging.Endpoint;
import messaging.Message;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class SecureEndpoint extends Endpoint {
    private final String MATERIAL = "CAFEBABECAFEBABE";
    private final String ENCODING_KEY = "AES";
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

    public void send(InetSocketAddress receiver, Serializable payload) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(payload);
            oos.flush();
            byte[] serializedPayload = bos.toByteArray();
            byte[] encodedPayload = encodingCipher.doFinal(serializedPayload);
            super.send(receiver, encodedPayload);
        } catch (IllegalBlockSizeException e) {
            throw new RuntimeException(e);
        } catch (BadPaddingException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public Message blockingReceive() {
        //TODO: implement decoding
        Message msg = super.blockingReceive();
        return msg;
    }

    public Message nonBlockingReceive() {
        //TODO: implement decoding
        Message msg = super.nonBlockingReceive();
        return msg;
    }
}