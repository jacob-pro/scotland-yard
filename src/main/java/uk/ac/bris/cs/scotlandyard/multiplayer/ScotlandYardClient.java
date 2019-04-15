package uk.ac.bris.cs.scotlandyard.multiplayer;

import com.google.gson.Gson;
import javafx.application.Platform;
import uk.ac.bris.cs.gamekit.graph.Graph;
import uk.ac.bris.cs.scotlandyard.model.*;
import uk.ac.bris.cs.scotlandyard.multiplayer.model.*;
import uk.ac.bris.cs.scotlandyard.multiplayer.network.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class ScotlandYardClient implements ClientDelegate, ScotlandYardGame {

	private CompletableFuture<Join> connectFuture = new CompletableFuture<>();
	private Set<ScotlandYardClientObserver> observers = new HashSet<>();
	private Set<Spectator> spectators = new HashSet<>();
	private Client client;
	private Gson gson = new Gson();

	private int currentRound = NOT_STARTED;
	private Colour currentPlayer;
	private Join joinMessage;
	private GameStart gameStart;
	private GameOver gameOver;

	public ScotlandYardClient(String hostname, int port, String username) throws URISyntaxException {
		URI uri = new URI("ws://" + hostname + ":" + port);
		Map<String,String> headers = new HashMap<>();
		headers.put("Username", username);
		headers.put("Version", ScotlandYardServer.protocolVersionString);
		this.client = new Client(uri, headers, 5000,this);
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
				this.tellObservers(o -> o.onLobbyChange(this, lobby));
				break;
			case GAME_START:
				this.gameStart = gson.fromJson(content, GameStart.class);
				this.tellObservers(o -> o.onGameStarted(this, this.gameStart));
				break;
			case MOVE_REQUEST:
				MoveRequest request = gson.fromJson(content, MoveRequest.class);
				this.currentPlayer = request.colour;
				this.tellObservers(o -> o.onMoveRequested(this, request));
				break;
			case MOVE_MADE:
				Move move = (Move) StringSerializer.deserializeObject(content);
				if (move != null) this.tellSpectators(s -> s.onMoveMade(this, move));
				break;
			case ROUND_STARTED:
				try {
					this.currentRound = Integer.parseInt(content);
					this.tellSpectators(o -> o.onRoundStarted(this, this.currentRound));
				} catch (NumberFormatException ignored) {}
				break;
			case ROTATION_COMPLETE:
				this.tellSpectators(o -> o.onRotationComplete(this));
				break;
			case GAME_OVER:
				this.gameOver = gson.fromJson(content, GameOver.class);
				this.tellSpectators(o -> o.onGameOver(this, this.getWinningPlayers()));
				break;
		}
	}

	@Override
	public void clientDidError(Client c, ConnectionException e) {
		this.tellObservers(o -> o.onClientError(this, e));
	}

	@Override
	public void clientWasDisconnected(Client c) {
		if (this.gameOver == null) {
			this.tellObservers(o -> o.onClientError(this, new RuntimeException("Connection closed unexpectedly")));
		}
	}

	private void tellObservers(Consumer<ScotlandYardClientObserver> tell) {
		Platform.runLater(() -> this.observers.forEach(tell));
	}

	private void tellSpectators(Consumer<Spectator> tell) {
		Platform.runLater(() -> this.spectators.forEach(tell));
	}

	public void registerObserver(ScotlandYardClientObserver observer) {
		this.observers.add(observer);
	}

	public void unregisterObserver(ScotlandYardClientObserver observer) {
		this.observers.remove(observer);
	}

	public Join joinMessage() {
		return this.joinMessage;
	}

	public void disconnect() {
		try {
			this.client.closeBlocking();
			System.out.println("ScotlandYardClient disconnected");
		} catch (InterruptedException ignored) {
		}
	}

	@Override
	public List<Colour> getPlayers() {
		if (this.gameStart == null) throw new RuntimeException("Game must be started");
		return this.gameStart.players.stream().map(p -> p.colour).collect(Collectors.toList());
	}

	@Override
	public Set<Colour> getWinningPlayers() {
		if (this.gameOver == null) return Collections.emptySet();
		return this.gameOver.winningPlayers;
	}

	@Override
	public Optional<Integer> getPlayerLocation(Colour colour) {
		try {
			String result = this.client.performRequest(RequestActions.GET_LOCATION.toString(), colour.toString()).get();
			return Optional.of(Integer.parseInt(result));
		} catch (InterruptedException | ExecutionException | NumberFormatException e) {
			this.tellObservers(o -> o.onClientError(this, new RuntimeException(e)));
			return Optional.empty();
		}
	}

	@Override
	public Optional<Integer> getPlayerTickets(Colour colour, Ticket ticket) {
		TicketRequest request = new TicketRequest(colour, ticket);
		try {
			String result = this.client.performRequest(RequestActions.GET_TICKETS.toString(), gson.toJson(request)).get();
			return Optional.of(Integer.parseInt(result));
		} catch (InterruptedException | ExecutionException | NumberFormatException e) {
			this.tellObservers(o -> o.onClientError(this, new RuntimeException(e)));
			return Optional.empty();
		}
	}

	@Override
	public boolean isGameOver() {
		return (this.gameOver != null);
	}

	@Override
	public Colour getCurrentPlayer() {
		return (this.currentPlayer != null) ? this.currentPlayer : this.gameStart.players.get(0).colour;
	}

	@Override
	public int getCurrentRound() {
		return this.currentRound;
	}

	@Override
	public List<Boolean> getRounds() {
		if (this.gameStart == null) throw new RuntimeException("Game must be started");
		return this.gameStart.rounds;
	}

	@Override
	public Graph<Integer, Transport> getGraph() {
		throw new RuntimeException("Unsupported - find the default graph from resources");
	}

	@Override
	public void startRotate() {
		throw new RuntimeException("Unsupported - this runs server side");
	}

	@Override
	public void registerSpectator(Spectator spectator) {
		this.spectators.add(spectator);
	}

	@Override
	public void unregisterSpectator(Spectator spectator) {
		this.spectators.remove(spectator);
	}

	@Override
	public Collection<Spectator> getSpectators() {
		return Collections.unmodifiableSet(this.spectators);
	}

}
