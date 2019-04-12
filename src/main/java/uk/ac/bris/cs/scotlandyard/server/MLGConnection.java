package uk.ac.bris.cs.scotlandyard.server;

import javafx.application.Platform;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import uk.ac.bris.cs.scotlandyard.model.Colour;
import uk.ac.bris.cs.scotlandyard.server.messaging.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class MLGConnection {

	private Set<MLGObserver> observers = new HashSet<>();
	private MLGConnectionInternal internal;

	public MLGConnection(String hostname, Integer port, String username) throws URISyntaxException {
		URI uri = new URI("ws://" + hostname + ":" + port.toString());
		Map<String,String> headers = new HashMap<>();
		headers.put("Username", username);
		headers.put("Version", MLGServer.protocolVersion.toString());
		this.internal = new MLGConnectionInternal(uri, headers);
	}

	public CompletableFuture<Join> connect() {
		this.internal.connect();
		return this.internal.connectFuture;
	}

	public CompletableFuture<Lobby> getLobby() {
		this.internal.performRequest(Action.GET_LOBBY, null);
		return null;
	}

	public CompletableFuture<?> setColour(Colour colour) {
		this.internal.performRequest(Action.SET_COLOUR, colour.toString());
		return null;
	}

	public CompletableFuture<?> setReady(Boolean ready) {
		this.internal.performRequest(Action.SET_READY, ready.toString());
		return null;
	}

	private void didReceiveNotification(Notification notification) {

	}

	private class MLGConnectionInternal extends WebSocketClient {

		private int streamIDCounter = 0;
		private Map<Integer, CompletableFuture<String>> pendingRequests;

		private CompletableFuture<Join> connectFuture = new CompletableFuture<>();
		private MessageDeserializer messageDeserializer = new MessageDeserializer();

		MLGConnectionInternal(URI uri, Map<String,String> headers) {
			super(uri, headers);
		}

		CompletableFuture<String> performRequest(Action action, String data) {
			CompletableFuture<String> future = new CompletableFuture<>();
			this.streamIDCounter++;
			Request request = new Request();
			request.streamID = this.streamIDCounter;
			request.data = data;
			request.action = action;
			this.pendingRequests.put(request.streamID, future);
			return future;
		}

		@Override
		public void onMessage(String string) {
			this.messageDeserializer.deserialize(string).ifPresent(m -> {
				m.accept(new MessageVisitor() {
					@Override
					public void accept(Join message) {
						if (message.error == null) {
							MLGConnectionInternal.this.connectFuture.complete(message);
						} else {
							MLGConnectionInternal.this.connectFuture.completeExceptionally(new MLGConnectionException(message.error.toString()));
						}
					}
					@Override
					public void accept(Response message) {
						CompletableFuture<String> future = MLGConnectionInternal.this.pendingRequests.get(message.streamID);
						if (future != null) {
							if (message.error == null) {
								future.complete(message.data);
							} else {
								future.completeExceptionally(new RequestException(message.error));
							}
						}
					}
					@Override
					public void accept(Notification message) {
						MLGConnection.this.didReceiveNotification(message);
					}
				});
			});
		}

		@Override
		public void onError(Exception e) {
			// If the error occurs on creation we will fail the creation promise e.g. couldn't connect to the server
			// I don't know what errors are possible later on, other than disconnection?
			// If so just throw a runtime exception on main thread
			if(!this.connectFuture.isDone()) {
				this.connectFuture.completeExceptionally(new MLGConnectionException(e));
			} else {
				Platform.runLater(() -> {
					throw new MLGConnectionException(e);
				});
			}
		}

		@Override
		public void onOpen(ServerHandshake handshake) {
		}

		@Override
		public void onClose(int code, String reason, boolean remote) {
		}
	}

	@SuppressWarnings("serial")
	public static class MLGConnectionException extends RuntimeException {
		MLGConnectionException(Exception e) {
			super(e);
		}
		MLGConnectionException(String s) {
			super(s);
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
