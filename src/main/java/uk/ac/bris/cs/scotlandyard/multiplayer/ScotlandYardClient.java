package uk.ac.bris.cs.scotlandyard.multiplayer;

import com.google.gson.Gson;
import javafx.application.Platform;
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

public class ScotlandYardClient implements ClientDelegate {

	private CompletableFuture<Join> connectFuture = new CompletableFuture<>();
	private Set<Observer> observers = new HashSet<>();
	private Client client;
	private Gson gson = new Gson();
	private boolean gameOver = false;
	private Join joinMessage;

	public ScotlandYardClient(String hostname, int port, String username) throws URISyntaxException {
		URI uri = new URI("ws://" + hostname + ":" + port);
		Map<String,String> headers = new HashMap<>();
		headers.put("Username", username);
		headers.put("Version", ScotlandYardServer.protocolVersionString);
		this.client = new Client(uri, headers, this);
	}

	public CompletableFuture<Join> connect() {
		this.client.connect();
		return this.connectFuture;
	}

	public CompletableFuture<Lobby> getLobby() {
		return this.client.performRequest(RequestActions.GET_LOBBY, null).thenApply(t -> gson.fromJson(t, Lobby.class));
	}

	@SuppressWarnings("UnusedReturnValue")
	public CompletableFuture<?> setColour(Colour colour) {
		String value = (colour == null) ? ScotlandYardServer.undecidedColour : colour.toString();
		return this.client.performRequest(RequestActions.SET_COLOUR, value);
	}

	@SuppressWarnings("UnusedReturnValue")
	public CompletableFuture<?> setReady(Boolean ready) {
		return this.client.performRequest(RequestActions.SET_READY, ready.toString());
	}

	@Override
	public void clientDidConnect(Client c, String data, ConnectionException e) {
		if (e == null) {
			Join join = gson.fromJson(data, Join.class);
			this.joinMessage = join;
			this.connectFuture.complete(join);
		} else {
			this.connectFuture.completeExceptionally(e);
		}
	}

	@Override
	public void clientReceivedNotification(Client c, String name, String content) {
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

	@Override
	public void clientDidError(Client c, ConnectionException e) {
		this.tellObservers(o -> o.onClientError(e));
	}

	@Override
	public void clientWasDisconnected(Client c) {
		if (!gameOver) {
			this.tellObservers(o -> o.onClientError(new RuntimeException("Connection closed unexpectedly")));
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
			this.client.closeBlocking();
		} catch (InterruptedException ignored) {

		}
	}

}
