package aqua.blatt1.common.msgtypes;

import java.io.Serializable;

@SuppressWarnings("serial")
public final class Token implements Serializable {
	private final String id;

	public Token(String id) {
		this.id = id;
	}

	public String getId() {
		return id;
	}
}
