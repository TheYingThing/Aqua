package aqua.blatt1.common.msgtypes;

import java.io.Serializable;

@SuppressWarnings("serial")
public class LocationRequest implements Serializable {

    private String fishId;

    public LocationRequest(String fishId) {
        this.fishId = fishId;
    }

    public String getFishId() {
        return this.fishId;
    }
}
