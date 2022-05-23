package aqua.blatt1.common;

import aqua.blatt1.common.msgtypes.KeyXChangeRequest;
import aqua.blatt1.common.msgtypes.KeyXChangeResponse;
import messaging.Endpoint;
import messaging.Message;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.*;
import java.net.InetSocketAddress;
import java.security.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

public class SecureEndpoint extends Endpoint {
    private static final String ENCODING_KEY = "RSA";
    private PublicKey publicKey;
    private Map<InetSocketAddress, PublicKey> knownKeys;
    private Map<InetSocketAddress, Stack<Serializable>> backlog;
    private Cipher privateCipher;

    public SecureEndpoint() {
        super();
        initCiphers();
        this.knownKeys = new HashMap<>();
        this.backlog = new HashMap<>();
    }

    public SecureEndpoint(int port) {
        super(port);
        initCiphers();
        this.knownKeys = new HashMap<>();
        this.backlog = new HashMap<>();
    }

    private void initCiphers() {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance(ENCODING_KEY);
            keyGen.initialize(1024);
            KeyPair pair = keyGen.generateKeyPair();
            PrivateKey privateKey = pair.getPrivate();
            this.publicKey = pair.getPublic();
            privateCipher = Cipher.getInstance(ENCODING_KEY);
            privateCipher.init(Cipher.DECRYPT_MODE, privateKey);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException e) {
            throw new RuntimeException(e);
        }
    }

    public Message decryptPayload(Message msg) {
        try {
            CipherText payloadCipher = (CipherText) msg.getPayload();
            byte[] decodedPayload = privateCipher.doFinal(payloadCipher.getCipherText());
            ByteArrayInputStream bos = new ByteArrayInputStream(decodedPayload);
            ObjectInputStream ois = new ObjectInputStream(bos);
            return new Message((Serializable) ois.readObject(), msg.getSender());
        } catch (IOException | IllegalBlockSizeException | BadPaddingException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public Serializable encryptPayload(InetSocketAddress receiver, Serializable payload) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(payload);
            oos.flush();
            byte[] serializedPayload = bos.toByteArray();
            Cipher encode = Cipher.getInstance(ENCODING_KEY);
            encode.init(Cipher.ENCRYPT_MODE, knownKeys.get(receiver));
            byte[] encodedPayload = encode.doFinal(serializedPayload);
            return new CipherText(encodedPayload);
        } catch (IllegalBlockSizeException | BadPaddingException | IOException | NoSuchPaddingException |
                 NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException(e);
        }
    }
    public void send(InetSocketAddress receiver, Serializable payload) {
        if(knownKeys.containsKey(receiver)) {
            super.send(receiver, encryptPayload(receiver, payload));
        } else {
            super.send(receiver, new KeyXChangeRequest(this.publicKey));
            if (!backlog.containsKey(receiver)) {
                backlog.put(receiver, new Stack<>());
            }
            backlog.get(receiver).push(payload);
        }

    }
    public Message blockingReceive() {
        Message msg;
        while(true) {
            msg = super.blockingReceive();
            InetSocketAddress sender = msg.getSender();
            if(msg.getPayload() instanceof KeyXChangeRequest) {
                knownKeys.put(sender, ((KeyXChangeRequest) msg.getPayload()).getPublicKey());
                super.send(sender, new KeyXChangeResponse(this.publicKey));
                continue;
            }
            if(msg.getPayload() instanceof KeyXChangeResponse) {
                knownKeys.put(sender, ((KeyXChangeResponse) msg.getPayload()).getPublicKey());
                while(!backlog.get(sender).isEmpty()) {
                    this.send(sender, backlog.get(sender).pop());
                }
                continue;
            }
            break;
        }
        return decryptPayload(msg);
    }

    public Message nonBlockingReceive() {
        Message msg;
        while(true) {
            msg = super.nonBlockingReceive();
            InetSocketAddress sender = msg.getSender();
            if(msg.getPayload() instanceof KeyXChangeRequest) {
                knownKeys.put(sender, ((KeyXChangeRequest) msg.getPayload()).getPublicKey());
                super.send(sender, new KeyXChangeResponse(this.publicKey));
                continue;
            }
            if(msg.getPayload() instanceof KeyXChangeResponse) {
                knownKeys.put(sender, ((KeyXChangeResponse) msg.getPayload()).getPublicKey());
                while(!backlog.get(sender).isEmpty()) {
                    this.send(sender, backlog.get(sender).pop());
                }
                continue;
            }
            break;
        }
        return decryptPayload(msg);
    }
}