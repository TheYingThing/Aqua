package aqua.blatt1.common.msgtypes;

import java.io.Serializable;

@SuppressWarnings("serial")
public final class RegisterResponse implements Serializable {
	private final String id;
	private final int lease;

	private final boolean reregister;

	public RegisterResponse(String id, int lease, boolean reregister) {
		this.id = id;
		this.lease = lease;
		this.reregister = reregister;
	}

	public String getId() {
		return id;
	}

	public int getLease() { return lease; }

	public boolean isReregister() { return reregister; }
}
