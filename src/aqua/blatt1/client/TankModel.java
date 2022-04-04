package aqua.blatt1.client;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import aqua.blatt1.common.Direction;
import aqua.blatt1.common.FishModel;

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
	protected final ClientCommunicator.ClientForwarder forwarder;

	public TankModel(ClientCommunicator.ClientForwarder forwarder) {
		this.fishies = Collections.newSetFromMap(new ConcurrentHashMap<FishModel, Boolean>());
		this.forwarder = forwarder;
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
		}
	}

	synchronized void receiveFish(FishModel fish) {
		fish.setToStart();
		fishies.add(fish);
	}

	public synchronized void updateNeighbors(InetSocketAddress left, InetSocketAddress right) {
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
					if(fish.getDirection() == Direction.LEFT) {
						forwarder.handOff(fish, this.leftNeighbor);
					} else {
						forwarder.handOff(fish, this.rightNeighbor);
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