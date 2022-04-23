package aqua.blatt1.common.msgtypes;

import java.io.Serializable;

@SuppressWarnings("serial")
public final class CollectionToken implements Serializable {
    private int totalFishies = 0;

    public void addSnapshot(int fishies) {
        totalFishies += fishies;
    }

    public int getTotalFishies() {
        return totalFishies;
    }
}
