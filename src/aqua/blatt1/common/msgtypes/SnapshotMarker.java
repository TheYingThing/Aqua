package aqua.blatt1.common.msgtypes;

import aqua.blatt1.client.AquaClient;

import java.io.Serializable;

@SuppressWarnings("serial")
public final class SnapshotMarker implements Serializable {
    private final AquaClient sender;

    public SnapshotMarker(AquaClient sender) {
        this.sender = sender;
    }

    public AquaClient getSender() { return sender;}
}
