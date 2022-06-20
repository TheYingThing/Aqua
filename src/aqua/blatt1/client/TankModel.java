package aqua.blatt1.client;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import aqua.blatt1.broker.AquaBroker;
import aqua.blatt1.common.Direction;
import aqua.blatt1.common.FishModel;
import aqua.blatt1.common.RecordingModus;
import aqua.blatt1.common.msgtypes.*;

import static aqua.blatt1.common.RecordingModus.*;


public class TankModel extends Observable implements Iterable<FishModel>, AquaClient {

	public static final int WIDTH = 600;
	public static final int HEIGHT = 350;
	protected static final int MAX_FISHIES = 5;
	protected static final Random rand = new Random();
	protected volatile String id;
	protected volatile AquaClient leftNeighbor;
	protected volatile AquaClient rightNeighbor;
	protected volatile boolean token = false;
	protected volatile Timer timer = new Timer();
	protected final Set<FishModel> fishies;
	protected final Map<String, AquaClient> homeAgent;
	protected int fishCounter = 0;
	protected RecordingModus modus = IDLE;
	protected CollectionToken collectionToken;
	protected int fishSnapshot = 0;
	protected int globalSnapshot = 0;
	protected boolean initiator = false;
	//protected final ClientCommunicator.ClientForwarder forwarder;
	protected final AquaBroker broker;
	protected AquaClient stub;

	public TankModel(AquaBroker broker) throws RemoteException, NotBoundException {
		this.fishies = Collections.newSetFromMap(new ConcurrentHashMap<FishModel, Boolean>());
		this.homeAgent = new HashMap<String, AquaClient>();
		this.broker = broker;
		this.stub = (AquaClient) UnicastRemoteObject.exportObject(this, 0);
	}

	synchronized void onRegistration(String id, int lease, boolean reregister) {
		this.id = id;
		if(!reregister) {
			newFish(WIDTH - FishModel.getXSize(), rand.nextInt(HEIGHT - FishModel.getYSize()));
		}
		TimerTask task = new TimerTask() {
			@Override
			public void run() {
				try {
					broker.handleRegisterRequest(new RegisterRequest(stub));
				} catch (RemoteException e) {
					throw new RuntimeException(e);
				}
				//forwarder.register(stub);
			}
		};
		timer.schedule(task, (lease - 1) * 1000L);
	}

	public synchronized void newFish(int x, int y) {
		if (fishies.size() < MAX_FISHIES) {
			x = Math.min(x, WIDTH - FishModel.getXSize() - 1);
			y = Math.min(y, HEIGHT - FishModel.getYSize());

			FishModel fish = new FishModel("fish" + (++fishCounter) + "@" + getId(), x, y,
					rand.nextBoolean() ? Direction.LEFT : Direction.RIGHT);

			fishies.add(fish);
			homeAgent.put(fish.getId(), null);
		}
	}

	synchronized void receiveFish(FishModel fish) throws RemoteException {
		fish.setToStart();
		fishies.add(fish);
		if(!modus.equals(IDLE)) {
			this.fishSnapshot++;
		}
		if(homeAgent.containsKey(fish)) {
			homeAgent.replace(fish.getId(), null);
		} else {
			broker.handleNameResolutionRequest(new NameResolutionRequest(fish.getTankId(), fish.getId()));
			//forwarder.sendNameResolutionRequest(fish.getTankId(), fish.getId());
		}
	}

	public synchronized void updateNeighbors(AquaClient left, AquaClient right) {
		if(left != null) {
			this.leftNeighbor = (left != this.leftNeighbor) ? left : null;
		}
		if(right != null) {
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
		for (Iterator<FishModel> it = iterator(); it.hasNext();) {
			FishModel fish = it.next();

			fish.update();

			if (fish.hitsEdge())
				if(hasToken()) {
					try {
						if(fish.getDirection() == Direction.LEFT) {
							leftNeighbor.handleHandoffRequest(new HandoffRequest(fish));
							//forwarder.handOff(fish, this.leftNeighbor);
							if(!modus.equals(IDLE)) {
								fishSnapshot--;
							}
						} else {
							rightNeighbor.handleHandoffRequest(new HandoffRequest(fish));
							//forwarder.handOff(fish, this.rightNeighbor);
							if(!modus.equals(IDLE)) {
								fishSnapshot--;
							}
						}
					} catch (RemoteException e) {
						throw new RuntimeException(e);
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
				try {
					leftNeighbor.handleToken();
				} catch (RemoteException e) {
					throw new RuntimeException(e);
				}
				//forwarder.sendToken(leftNeighbor);
			}
		};
		timer.schedule(task, 2000);
	}

	public synchronized boolean hasToken() {
		return this.token;
	}

	public synchronized void initiateSnapshot() throws RemoteException {
		this.initiator = true;
		this.modus = BOTH;
		this.fishSnapshot = fishCounter;
		leftNeighbor.handleSnapshotMarker(new SnapshotMarker(stub));
		rightNeighbor.handleSnapshotMarker(new SnapshotMarker(stub));
		leftNeighbor.handleCollectionToken(new CollectionToken());
		//forwarder.sendMarker(leftNeighbor);
		//forwarder.sendMarker(rightNeighbor);
		//forwarder.sendCollectionToken(leftNeighbor, new CollectionToken());
	}

	public synchronized void forwardCollectionToken() throws RemoteException {
		collectionToken.addSnapshot(fishSnapshot);
		leftNeighbor.handleCollectionToken(collectionToken);
		//forwarder.sendCollectionToken(leftNeighbor, collectionToken);
	}

	public synchronized void receiveCollectionToken(CollectionToken token) throws RemoteException {
		collectionToken = token;
		if (modus.equals(IDLE) && !initiator) {
			forwardCollectionToken();
		} else if (initiator) {
			collectionToken.addSnapshot(fishSnapshot);
			globalSnapshot = collectionToken.getTotalFishies();
		}
	}

	public synchronized void receiveMarker(AquaClient sender) throws RemoteException {
		if(modus.equals(IDLE)) {
			modus = sender.equals(rightNeighbor) ? LEFT : RIGHT;
			this.fishSnapshot = fishCounter;
			leftNeighbor.handleSnapshotMarker(new SnapshotMarker(stub));
			rightNeighbor.handleSnapshotMarker(new SnapshotMarker(stub));
		} else {
			switch(modus) {
				case BOTH:
					modus = sender.equals(rightNeighbor) ? LEFT : RIGHT;
					break;
				default:
					modus = IDLE;
					if(collectionToken != null && !initiator) {
						forwardCollectionToken();
					}
			}
		}
	}

	public synchronized void receiveNameResolutionResponse(NameResolutionResponse msg) throws RemoteException {
		msg.getTankAddress().handleLocationUpdate(new LocationUpdate(msg.getRequestID(), msg.getTankAddress()));
	}

	public synchronized void updateFishLocation(AquaClient location, String fishID) {
		homeAgent.put(fishID, location);
	}

	public synchronized void locateFishGlobally(String fishID) throws RemoteException {
		if(homeAgent.get(fishID) == null) {
			locateFishLocally(fishID);
		} else {
			homeAgent.get(fishID).handleLocationRequest(new LocationRequest(fishID));
		}
	}

	public synchronized void locateFishLocally(String fishID) {
		FishModel fish = fishies.stream().filter(f -> f.getId().equals(fishID)).findFirst().get();
		fish.toggle();
	}

	private synchronized void update() {
		updateFishies();
		setChanged();
		notifyObservers();
	}

	protected void run() throws RemoteException {
		RegisterRequest registerRequest = new RegisterRequest(this.stub);
		broker.handleRegisterRequest(registerRequest);
		try {
			while (!Thread.currentThread().isInterrupted()) {
				update();
				TimeUnit.MILLISECONDS.sleep(10);
			}
		} catch (InterruptedException consumed) {
			// allow method to terminate
		}
	}

	public synchronized void finish() throws RemoteException {
		if(hasToken()) {
			leftNeighbor.handleToken();
		}
		broker.handleDeregisterRequest(new DeregisterRequest(id));
	}

	@Override
	public void handleRegisterResponse(RegisterResponse registerResponse) throws RemoteException {
		onRegistration(registerResponse.getId(), registerResponse.getLease(), registerResponse.isReregister());
	}

	@Override
	public void handleHandoffRequest(HandoffRequest handoffRequest) throws RemoteException {
		receiveFish(handoffRequest.getFish());
	}

	@Override
	public void handleNeighborUpdate(NeighborUpdate neighborUpdate) throws RemoteException {
		updateNeighbors(neighborUpdate.getLeft(), neighborUpdate.getRight());
	}

	@Override
	public void handleToken() throws RemoteException {
		receiveToken();
	}

	@Override
	public void handleSnapshotMarker(SnapshotMarker snapshotMarker) throws RemoteException {
		receiveMarker(snapshotMarker.getSender());
	}

	@Override
	public void handleCollectionToken(CollectionToken collectionToken) throws RemoteException {
		receiveCollectionToken(collectionToken);
	}

	@Override
	public void handleLocationRequest(LocationRequest locationRequest) throws RemoteException {
		locateFishLocally(locationRequest.getFishID());
	}

	@Override
	public void handleNameResolutionResponse(NameResolutionResponse nameResolutionResponse) throws RemoteException {
		receiveNameResolutionResponse(nameResolutionResponse);
	}

	@Override
	public void handleLocationUpdate(LocationUpdate locationUpdate) throws RemoteException {
		updateFishLocation(locationUpdate.getSender(), locationUpdate.getFishID());
	}
}