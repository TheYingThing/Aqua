package aqua.blatt1.broker;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

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
		public Instant timestamp;

		Client(String id, T client, Instant timestamp) {
			this.id = id;
			this.client = client;
			this.leftNeighbor = null;
			this.rightNeighbor = null;
			this.timestamp = timestamp;
		}
	}

	private final List<Client> clients;

	public ClientCollection() {
		clients = new ArrayList<Client>();
	}

	public ClientCollection<T> add(String id, T client, Instant timestamp) {
		clients.add(new Client(id, client, timestamp));
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

	public String getClientId(int index) {
		return clients.get(index).id;
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

	public void replaceTimestamp(int index, Instant timestamp) {
		clients.get(index).timestamp = timestamp;
	}

	public Instant getTimestamp(int index) {
		return clients.get(index).timestamp;
	}
}
