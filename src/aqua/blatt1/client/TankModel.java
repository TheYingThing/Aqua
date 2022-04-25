package aqua.blatt1.client;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import aqua.blatt1.common.Direction;
import aqua.blatt1.common.FishLocation;
import aqua.blatt1.common.FishModel;
import aqua.blatt1.common.RecordingModus;
import aqua.blatt1.common.msgtypes.CollectionToken;

import static aqua.blatt1.common.RecordingModus.*;
import static aqua.blatt1.common.FishLocation.*;

public class TankModel extends Observable implements Iterable<FishModel> {

    public static final int WIDTH = 600;
    public static final int HEIGHT = 350;
    protected static final int MAX_FISHIES = 5;
    protected static final Random rand = new Random();
    protected volatile String id;
    protected volatile InetSocketAddress leftNeighbor;
    protected volatile InetSocketAddress rightNeighbor;
    protected volatile boolean token = false;
    protected volatile Timer timer = new Timer();
    protected final Set<FishModel> fishies;
    protected int fishCounter = 0;
    protected RecordingModus modus = IDLE;
    protected CollectionToken collectionToken;
    protected int fishSnapshot = 0;
    protected int globalSnapshot = 0;
    protected boolean initiator = false;
    protected final ClientCommunicator.ClientForwarder forwarder;
    protected volatile Map<String, FishLocation> fishLocationMap;

    public TankModel(ClientCommunicator.ClientForwarder forwarder) {
        this.fishies = Collections.newSetFromMap(new ConcurrentHashMap<FishModel, Boolean>());
        this.forwarder = forwarder;
        this.fishLocationMap = new HashMap<>();
    }

    synchronized void onRegistration(String id) {
        this.id = id;
        newFish(WIDTH - FishModel.getXSize(), rand.nextInt(HEIGHT - FishModel.getYSize()));
    }

    public synchronized void newFish(int x, int y) {
        if (fishies.size() < MAX_FISHIES) {
            x = x > WIDTH - FishModel.getXSize() - 1 ? WIDTH - FishModel.getXSize() - 1 : x;
            y = y > HEIGHT - FishModel.getYSize() ? HEIGHT - FishModel.getYSize() : y;

            FishModel fish = new FishModel("fish" + (++fishCounter) + "@" + getId(), x, y,
                    rand.nextBoolean() ? Direction.LEFT : Direction.RIGHT);

            fishies.add(fish);
            fishLocationMap.put(fish.getId(), HERE);
        }
    }

    synchronized void receiveFish(FishModel fish) {
        fish.setToStart();
        fishies.add(fish);
        fishLocationMap.put(fish.getId(), HERE);
        if (!modus.equals(IDLE)) {
            this.fishSnapshot++;
        }
    }

    public synchronized void updateNeighbors(InetSocketAddress left, InetSocketAddress right) {
        if (left != null) {
            this.leftNeighbor = (left != this.leftNeighbor) ? left : null;
        }
        if (right != null) {
            this.rightNeighbor = (right != this.rightNeighbor) ? right : null;
        }
    }

    public String getId() {
        return id;
    }

    public synchronized int getFishCounter() {
        return fishCounter;
    }

    public synchronized Iterator<FishModel> iterator() {
        return fishies.iterator();
    }

    private synchronized void updateFishies() {
        for (Iterator<FishModel> it = iterator(); it.hasNext(); ) {
            FishModel fish = it.next();

            fish.update();

            if (fish.hitsEdge())
                if (hasToken()) {
                    if (fish.getDirection() == Direction.LEFT) {
                        forwarder.handOff(fish, this.leftNeighbor);
                        fishLocationMap.replace(fish.getId(), FishLocation.LEFT);
                        if (!modus.equals(IDLE)) {
                            fishSnapshot--;
                        }
                    } else {
                        forwarder.handOff(fish, this.rightNeighbor);
                        fishLocationMap.replace(fish.getId(), FishLocation.RIGHT);
                        if (!modus.equals(IDLE)) {
                            fishSnapshot--;
                        }
                    }
                } else {
                    fish.reverse();
                }

            if (fish.disappears())
                it.remove();
        }
    }

    public synchronized void receiveToken() {
        this.token = true;
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                token = false;
                forwarder.sendToken(leftNeighbor);
            }
        };
        timer.schedule(task, 2000);
    }

    public synchronized boolean hasToken() {
        return this.token;
    }

    public synchronized void initiateSnapshot() {
        this.initiator = true;
        this.modus = BOTH;
        this.fishSnapshot = fishCounter;
        forwarder.sendMarker(leftNeighbor);
        forwarder.sendMarker(rightNeighbor);
        forwarder.sendCollectionToken(leftNeighbor, new CollectionToken());
    }

    public synchronized void forwardCollectionToken() {
        collectionToken.addSnapshot(fishSnapshot);
        forwarder.sendCollectionToken(leftNeighbor, collectionToken);
    }

    public synchronized void receiveCollectionToken(CollectionToken token) {
        collectionToken = token;
        if (modus.equals(IDLE) && !initiator) {
            forwardCollectionToken();
        } else if (initiator) {
            collectionToken.addSnapshot(fishSnapshot);
            globalSnapshot = collectionToken.getTotalFishies();
        }
    }

    public synchronized void receiveMarker(InetSocketAddress sender) {
        if (modus.equals(IDLE)) {
            modus = sender.equals(rightNeighbor) ? RecordingModus.LEFT : RecordingModus.RIGHT;
            this.fishSnapshot = fishCounter;
            forwarder.sendMarker(leftNeighbor);
            forwarder.sendMarker(rightNeighbor);
        } else {
            switch (modus) {
                case BOTH:
                    modus = sender.equals(rightNeighbor) ? RecordingModus.LEFT : RecordingModus.RIGHT;
                    break;
                default:
                    modus = IDLE;
                    if (collectionToken != null && !initiator) {
                        forwardCollectionToken();
                    }
            }
        }
    }

    public void locateFishGlobally(String fish) {
        FishLocation location = fishLocationMap.get(fish);
        switch (location) {
            case LEFT:
                forwarder.sendLocationRequest(leftNeighbor, fish);
                break;
            case RIGHT:
                forwarder.sendLocationRequest(rightNeighbor, fish);
                break;
            default:
                locateFishLocally(fish);
                break;
        }
    }

    private void locateFishLocally(String fishId) {
        fishies.stream()
                .filter(fishModel -> fishId.equals(fishModel.getId()))
                .forEach(FishModel::toggle);
    }

    private synchronized void update() {
        updateFishies();
        setChanged();
        notifyObservers();
    }

    protected void run() {
        forwarder.register();

        try {
            while (!Thread.currentThread().isInterrupted()) {
                update();
                TimeUnit.MILLISECONDS.sleep(10);
            }
        } catch (InterruptedException consumed) {
            // allow method to terminate
        }
    }

    public synchronized void finish() {
        forwarder.deregister(id);
    }

}