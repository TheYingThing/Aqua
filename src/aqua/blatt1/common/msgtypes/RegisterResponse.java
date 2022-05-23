package aqua.blatt1.common.msgtypes;

import java.io.Serializable;

@SuppressWarnings("serial")
public final class RegisterResponse implements Serializable {
	private final String id;
	private final int leaseTime;
	private final boolean newTank;

	public RegisterResponse(String id, int leaseTime, boolean newTank) {
		this.id = id;
		this.leaseTime = leaseTime;
		this.newTank = newTank;
	}

	public String getId() {
		return id;
	}

	public int getLeaseTime() {
		return leaseTime;
	}

	public boolean isNewTank() {
		return newTank;
	}
}
