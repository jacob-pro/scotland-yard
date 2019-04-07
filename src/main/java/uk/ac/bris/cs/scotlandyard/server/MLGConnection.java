package uk.ac.bris.cs.scotlandyard.server;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.CompletableFuture;

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

	private Set<MLGObserver> observers = Collections.newSetFromMap(new WeakHashMap<>());
	private CompletableFuture<MLGConnection> connectFuture = new CompletableFuture<>();

	private MLGConnection(URI uri) {
		super(uri);
	}

	@Override
	public void onOpen( ServerHandshake handshakedata ) {
		this.connectFuture.complete(this);
	}

	@Override
	public void onMessage( String message ) {
		System.out.println( "received: " + message );
	}

	@Override
	public void onClose( int code, String reason, boolean remote ) {

		// The codecodes are documented in class org.java_websocket.framing.CloseFrame
		System.out.println( "Connection closed by " + ( remote ? "remote peer" : "us" ) + " Code: " + code + " Reason: " + reason );
	}

	@Override
	public void onError( Exception ex ) {
		if(!this.connectFuture.isDone()) {
			this.connectFuture.completeExceptionally(ex);
		}
		// if the error is fatal then onClose will be called additionally
	}

	public void registerObserver(MLGObserver observer) {
		this.observers.add(observer);
	}

	public void unregisterObserver(MLGObserver observer) {
		this.observers.remove(observer);
	}

}
