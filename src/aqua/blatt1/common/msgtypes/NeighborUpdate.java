package aqua.blatt1.common.msgtypes;

import java.io.Serializable;
import java.net.InetSocketAddress;

@SuppressWarnings("serial")
public final class NeighborUpdate implements Serializable {
	private final InetSocketAddress left;
	private final InetSocketAddress right;

	public NeighborUpdate(InetSocketAddress left, InetSocketAddress right) {
		this.left = left;
		this.right = right;
	}

	public InetSocketAddress getLeft() {
		return left;
	}

	public InetSocketAddress getRight() {
		return right;
	}
}
