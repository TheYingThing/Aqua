package aqua.blatt1.broker;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/*
 * This class is not thread-safe and hence must be used in a thread-safe way, e.g. thread confined or 
 * externally synchronized. 
 */

public class ClientCollection<T> {
	private class Client {
		final String id;
		final T client;
		public T leftNeighbor;
		public T rightNeighbor;

		public Instant regTime;

		Client(String id, T client, Instant regTime) {
			this.id = id;
			this.client = client;
			this.leftNeighbor = null;
			this.rightNeighbor = null;
			this.regTime = regTime;
		}

		public void setRegTime(Instant regTime) {
			this.regTime = regTime;
		}

		public Instant getRegTime() {
			return this.regTime;
		}
	}

	private final List<Client> clients;

	public ClientCollection() {
		clients = new ArrayList<Client>();
	}

	public ClientCollection<T> add(String id, T client) {
		clients.add(new Client(id, client, Instant.now()));
		return this;
	}

	public ClientCollection<T> remove(int index) {
		clients.remove(index);
		return this;
	}

	public int indexOf(String id) {
		for (int i = 0; i < clients.size(); i++)
			if (clients.get(i).id.equals(id))
				return i;
		return -1;
	}

	public int indexOf(T client) {
		for (int i = 0; i < clients.size(); i++)
			if (clients.get(i).client.equals(client))
				return i;
		return -1;
	}

	public T getClient(int index) {
		return clients.get(index).client;
	}

	public String getId(int index) {
		return clients.get(index).id;
	}

	public void setRegTime(int index, Instant regTime) {
		clients.get(index).setRegTime(regTime);
	}

	public Instant getRegTime(int index) {
		return clients.get(index).getRegTime();
	}

	public int size() {
		return clients.size();
	}

	public T getLeftNeighorOf(int index) {
		return index == 0 ? clients.get(clients.size() - 1).client : clients.get(index - 1).client;
	}

	public T getRightNeighorOf(int index) {
		return index < clients.size() - 1 ? clients.get(index + 1).client : clients.get(0).client;
	}

}
