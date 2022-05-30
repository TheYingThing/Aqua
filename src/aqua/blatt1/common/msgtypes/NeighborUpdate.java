package aqua.blatt1.common.msgtypes;

import aqua.blatt1.client.AquaClient;

import java.io.Serializable;
import java.net.InetSocketAddress;

@SuppressWarnings("serial")
public final class NeighborUpdate implements Serializable {
	private final AquaClient left;
	private final AquaClient right;

	public NeighborUpdate(AquaClient left, AquaClient right) {
		this.left = left;
		this.right = right;
	}

	public AquaClient getLeft() {
		return left;
	}

	public AquaClient getRight() {
		return right;
	}
}
