package uk.ac.bris.cs.scotlandyard.server;

import uk.ac.bris.cs.scotlandyard.model.Spectator;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

public class MLGClient {

	Set<MLGObserver> observers = Collections.newSetFromMap(new WeakHashMap<MLGObserver, Boolean>());

	private InetSocketAddress address;

	public MLGClient(InetSocketAddress address) {
		this.address = address;
	}

	public void connect() {

	}

	public void registerObserver(MLGObserver observer) {
		this.observers.add(observer);
	}

	public void unregisterObserver(MLGObserver observer) {
		this.observers.remove(observer);
	}

}
