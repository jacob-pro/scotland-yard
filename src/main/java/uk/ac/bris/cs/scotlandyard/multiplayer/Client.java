package uk.ac.bris.cs.scotlandyard.multiplayer;

import com.google.gson.Gson;
import javafx.application.Platform;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import uk.ac.bris.cs.scotlandyard.model.Colour;
import uk.ac.bris.cs.scotlandyard.multiplayer.network.*;
import uk.ac.bris.cs.scotlandyard.multiplayer.model.Join;
import uk.ac.bris.cs.scotlandyard.multiplayer.model.Lobby;
import uk.ac.bris.cs.scotlandyard.multiplayer.model.NotificationNames;
import uk.ac.bris.cs.scotlandyard.multiplayer.model.RequestActions;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class Client {

	private Set<Observer> observers = new HashSet<>();
	private MLGConnectionInternal internal;
	private Gson gson = new Gson();
	private Join joinMessage;
	private boolean gameOver = false;

	public Client(String hostname, Integer port, String username) throws URISyntaxException {
		URI uri = new URI("ws://" + hostname + ":" + port.toString());
		Map<String,String> headers = new HashMap<>();
		headers.put("Username", username);
		headers.put("Version", ScotlandYardServer.protocolVersionString);
		this.internal = new MLGConnectionInternal(uri, headers);
	}

	public CompletableFuture<Join> connect() {
		this.internal.connect();
		return this.internal.connectFuture.thenApply(t -> {
			Join join = gson.fromJson(t.data, Join.class);
			this.joinMessage = join;
			return join;
		});
	}

	public CompletableFuture<Lobby> getLobby() {
		return this.internal.performRequest(RequestActions.GET_LOBBY, null).thenApply(t -> gson.fromJson(t, Lobby.class));
	}

	@SuppressWarnings("UnusedReturnValue")
	public CompletableFuture<?> setColour(Colour colour) {
		String value = (colour == null) ? ScotlandYardServer.undecidedColour : colour.toString();
		return this.internal.performRequest(RequestActions.SET_COLOUR, value);
	}

	@SuppressWarnings("UnusedReturnValue")
	public CompletableFuture<?> setReady(Boolean ready) {
		return this.internal.performRequest(RequestActions.SET_READY, ready.toString());
	}

	private void didReceiveNotification(String name, String content) {
		NotificationNames type = Arrays.stream(NotificationNames.values()).filter(v -> v.toString().equals(name)).findAny().orElse(null);
		if (type == null) return;
		switch (type) {
			case LOBBY_UPDATE:
				Lobby lobby = gson.fromJson(content, Lobby.class);
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
				this.gameOver = true;
				break;
		}
	}

	//The internal class handles communication, but not game logic
	private class MLGConnectionInternal extends WebSocketClient {

		private Counter streamIDCounter = new Counter();
		private Map<Integer, CompletableFuture<String>> pendingRequests = new HashMap<>();

		private CompletableFuture<Handshake> connectFuture = new CompletableFuture<>();
		private MessageDeserializer messageDeserializer = new MessageDeserializer();

		MLGConnectionInternal(URI uri, Map<String,String> headers) {
			super(uri, headers);
		}

		CompletableFuture<String> performRequest(Object action, String data) {
			CompletableFuture<String> future = new CompletableFuture<>();
			Request request = new Request();
			request.streamID = this.streamIDCounter.next();
			request.data = data;
			request.action = action.toString();
			this.pendingRequests.put(request.streamID, future);
			this.send(Client.this.gson.toJson(request));
			return future;
		}

		@Override
		public void onMessage(String string) {
			this.messageDeserializer.deserialize(string).ifPresent(m -> m.accept(new MessageVisitor() {
				@Override
				public void accept(Handshake message) {
					if (message.error == null) {
						MLGConnectionInternal.this.connectFuture.complete(message);
					} else {
						MLGConnectionInternal.this.connectFuture.completeExceptionally(new ConnectionException(message.error));
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
					if (message.name == null) return;
					Client.this.didReceiveNotification(message.name, message.content);
				}
			}));
		}

		@Override
		public void onError(Exception e) {
			// If the error occurs on creation we will fail the creation promise e.g. couldn't connect to the network
			// I don't really know what sort of errors are possible later on, other than disconnection?
			if(!this.connectFuture.isDone()) {
				this.connectFuture.completeExceptionally(new ConnectionException(e));
			} else {
				Platform.runLater(() -> Client.this.tellObservers(o -> o.onConnectionError(new ConnectionException(e))));
			}
		}

		@Override
		public void onOpen(ServerHandshake handshake) {
		}

		@Override
		public void onClose(int code, String reason, boolean remote) {
			if(remote && !Client.this.gameOver) {
				Client.this.tellObservers(o -> o.onConnectionError(new ConnectionException("Connection closed unexpectedly")));
			}
		}
	}

	private void tellObservers(Consumer<Observer> tell) {
		Platform.runLater(() -> this.observers.forEach(tell));
	}

	public void registerObserver(Observer observer) {
		this.observers.add(observer);
	}

	public void unregisterObserver(Observer observer) {
		this.observers.remove(observer);
	}

	public Join joinMessage() {
		return this.joinMessage;
	}

	public void close() {
		try {
			this.internal.closeBlocking();
		} catch (InterruptedException ignored) {

		}
	}

}
