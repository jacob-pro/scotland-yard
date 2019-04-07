package uk.ac.bris.cs.scotlandyard.server;

import javafx.application.Platform;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class MLGConnection extends WebSocketClient {

	public static CompletableFuture<MLGConnection> CreateMLGConnection(String hostname, Integer port) {
		try {
			URI uri = new URI("ws://" + hostname + ":" + port.toString());
			MLGConnection client = new MLGConnection(uri);
			client.connect();
			return client.connectFuture;
		} catch (URISyntaxException e) {
			CompletableFuture<MLGConnection> connectFuture = new CompletableFuture<>();
			connectFuture.completeExceptionally(e);
			return connectFuture;
		}
	}

	private Set<MLGObserver> observers = new HashSet<>();
	private CompletableFuture<MLGConnection> connectFuture = new CompletableFuture<>();

	private MLGConnection(URI uri) {
		super(uri);
	}

	@Override
	public void onOpen(ServerHandshake handshake) {
		this.connectFuture.complete(this);
	}

	@Override
	public void onMessage(String message) {
		System.out.println( "received: " + message );
	}

	@Override
	public void onClose(int code, String reason, boolean remote) {
	}

	@Override
	public void onError(Exception ex) {
		// if the error is fatal then onClose will be called additionally
		if(!this.connectFuture.isDone()) {
			this.connectFuture.completeExceptionally(ex);
		}
	}

	private void tellObservers(Consumer<MLGObserver> tell) {
		Platform.runLater(() -> {
			this.observers.forEach(tell);
		});
	}

	public void registerObserver(MLGObserver observer) {
		this.observers.add(observer);
	}

	public void unregisterObserver(MLGObserver observer) {
		this.observers.remove(observer);
	}

}
