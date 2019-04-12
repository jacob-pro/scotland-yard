package uk.ac.bris.cs.scotlandyard.server;

import com.google.gson.Gson;
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

public class Client {

	private Set<Observer> observers = new HashSet<>();
	private MLGConnectionInternal internal;
	private Gson gson = new Gson();

	public Client(String hostname, Integer port, String username) throws URISyntaxException {
		URI uri = new URI("ws://" + hostname + ":" + port.toString());
		Map<String,String> headers = new HashMap<>();
		headers.put("Username", username);
		headers.put("Version", Server.protocolVersionString);
		this.internal = new MLGConnectionInternal(uri, headers);
	}

	public CompletableFuture<Join> connect() {
		this.internal.connect();
		return this.internal.connectFuture;
	}

	public CompletableFuture<Lobby> getLobby() {
		return this.internal.performRequest(Request.Action.GET_LOBBY, null).thenApply(t -> gson.fromJson(t, Lobby.class));
	}

	public CompletableFuture<?> setColour(Colour colour) {
		return this.internal.performRequest(Request.Action.SET_COLOUR, colour.toString());
	}

	public CompletableFuture<?> setReady(Boolean ready) {
		return this.internal.performRequest(Request.Action.SET_READY, ready.toString());
	}

	private void didReceiveNotification(Notification notification) {
		if (notification.name == null) return;
		switch (notification.name) {
			case LOBBY_UPDATE:
				Lobby lobby = gson.fromJson(notification.content, Lobby.class);
				this.tellObservers(o -> o.onLobbyChange(lobby));
				break;
			case GAME_START:
				this.tellObservers(Observer::onGameStarted);
				break;
			case MOVE_REQUEST:
				break;
			case MOVE_MADE:
				break;
			case ROUND_STARTED:
				break;
			case ROTATION_COMPLETE:
				this.tellObservers(o -> o.onRotationComplete(null));
				break;
			case GAME_OVER:
				break;
		}
	}

	private class MLGConnectionInternal extends WebSocketClient {

		private Counter streamIDCounter = new Counter();
		private Map<Integer, CompletableFuture<String>> pendingRequests = new HashMap<>();

		private CompletableFuture<Join> connectFuture = new CompletableFuture<>();
		private MessageDeserializer messageDeserializer = new MessageDeserializer();

		MLGConnectionInternal(URI uri, Map<String,String> headers) {
			super(uri, headers);
		}

		CompletableFuture<String> performRequest(Request.Action action, String data) {
			CompletableFuture<String> future = new CompletableFuture<>();
			Request request = new Request();
			request.streamID = this.streamIDCounter.next();
			request.data = data;
			request.action = action;
			this.pendingRequests.put(request.streamID, future);
			this.send(Client.this.gson.toJson(request));
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
						Client.this.didReceiveNotification(message);
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

	private void tellObservers(Consumer<Observer> tell) {
		Platform.runLater(() -> {
			this.observers.forEach(tell);
		});
	}

	public void registerObserver(Observer observer) {
		this.observers.add(observer);
	}

	public void unregisterObserver(Observer observer) {
		this.observers.remove(observer);
	}

}
