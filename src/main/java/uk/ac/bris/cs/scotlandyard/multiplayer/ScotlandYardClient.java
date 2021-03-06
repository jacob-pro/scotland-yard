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

//The client will fail without attempting recovery if unexpected input is received from the server (e.g. null value)
//If unexpected input is received as a notification the client will fail with the onClientError notification
//If unexpected input is received for synchronous calls then they will throw
public class ScotlandYardClient implements ClientDelegate, ScotlandYardGame {

    private final CompletableFuture<Join> connectFuture = new CompletableFuture<>();
    private final Set<ScotlandYardClientObserver> observers = new HashSet<>();
    private final Set<Spectator> spectators = new HashSet<>();
    private final Client client;
    private final Gson gson = new Gson();

    // Access should be synchronised between Websocket and UI threads
    private int currentRound = NOT_STARTED;
    private Colour currentPlayer;
    private GameStart gameStart;
    private GameOver gameOver;

    public ScotlandYardClient(String hostname, int port, String username) throws URISyntaxException {
        URI uri = new URI("ws://" + hostname + ":" + port);
        Map<String, String> headers = new HashMap<>();
        headers.put("Username", username);
        headers.put("Version", ScotlandYardServer.protocolVersionString);
        this.client = new Client(uri, headers, 5000, this);
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
        String value = (colour == null) ? null : colour.toString();
        return this.client.performRequest(RequestActions.SET_COLOUR, value);
    }

    @SuppressWarnings("UnusedReturnValue")
    public CompletableFuture<?> setReady(Boolean ready) {
        return this.client.performRequest(RequestActions.SET_READY, ready.toString());
    }

    @SuppressWarnings("UnusedReturnValue")
    public CompletableFuture<?> makeMove(Move move) {
        return this.client.performRequest(RequestActions.MAKE_MOVE, StringSerializer.serializeObject(move));
    }

    @Override
    public void clientDidConnect(Client c, String data, ConnectionException e) {
        if (e == null) {
            Join join = gson.fromJson(data, Join.class);
            this.connectFuture.complete(join);
        } else {
            this.connectFuture.completeExceptionally(e);
        }
    }

    @Override
    public synchronized void clientReceivedNotification(Client c, String name, String content) {
        try {
            NotificationNames type = Arrays.stream(NotificationNames.values()).filter(v -> v.toString().equals(name)).findAny().orElseThrow();
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
                    MoveMade moveMade = gson.fromJson(content, MoveMade.class);
                    this.currentPlayer = moveMade.currentPlayer;
                    this.tellSpectators(s -> s.onMoveMade(this, moveMade.getMove()));
                    break;
                case ROUND_STARTED:
                    this.currentRound = Integer.parseInt(content);
                    this.tellSpectators(o -> o.onRoundStarted(this, this.currentRound));
                    break;
                case ROTATION_COMPLETE:
                    this.tellSpectators(o -> o.onRotationComplete(this));
                    break;
                case GAME_OVER:
                    this.gameOver = gson.fromJson(content, GameOver.class);
                    this.tellSpectators(o -> o.onGameOver(this, this.getWinningPlayers()));
                    break;
            }
        } catch (RuntimeException e) {
            this.tellObservers(o -> o.onClientError(this, e));
        }
    }

    @Override
    public void clientDidError(Client c, ConnectionException e) {
        this.tellObservers(o -> o.onClientError(this, e));
    }

    @Override
    public synchronized void clientWasDisconnected(Client c) {
        if (this.gameOver == null) {
            this.tellObservers(o -> o.onClientError(this, new RuntimeException("Connection closed unexpectedly")));
        }
    }

    private synchronized void tellObservers(Consumer<ScotlandYardClientObserver> tell) {
        Platform.runLater(() -> this.observers.forEach(tell));
    }

    private synchronized void tellSpectators(Consumer<Spectator> tell) {
        Platform.runLater(() -> this.spectators.forEach(tell));
    }

    public synchronized void registerObserver(ScotlandYardClientObserver observer) {
        this.observers.add(observer);
    }

    public synchronized void unregisterObserver(ScotlandYardClientObserver observer) {
        this.observers.remove(observer);
    }

    public void disconnect() {
        try {
            this.client.closeBlocking();
            System.out.println("ScotlandYardClient disconnected");
        } catch (InterruptedException ignored) {
        }
    }

    @Override
    public synchronized List<Colour> getPlayers() {
        if (this.gameStart == null) throw new RuntimeException("Game must be started");
        return this.gameStart.players.stream().map(p -> p.colour).collect(Collectors.toList());
    }

    @Override
    public synchronized Set<Colour> getWinningPlayers() {
        if (this.gameOver == null) return Collections.emptySet();
        return this.gameOver.winningPlayers;
    }

    @Override
    public Optional<Integer> getPlayerLocation(Colour colour) {
        try {
            String result = this.client.performRequest(RequestActions.GET_LOCATION.toString(), colour.toString()).get();
            return Optional.of(Integer.parseInt(result));
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Optional<Integer> getPlayerTickets(Colour colour, Ticket ticket) {
        TicketRequest request = new TicketRequest(colour, ticket);
        try {
            String result = this.client.performRequest(RequestActions.GET_TICKETS.toString(), gson.toJson(request)).get();
            return Optional.of(Integer.parseInt(result));
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public synchronized boolean isGameOver() {
        return (this.gameOver != null);
    }

    @Override
    public synchronized Colour getCurrentPlayer() {
        return (this.currentPlayer != null) ? this.currentPlayer : this.gameStart.players.get(0).colour;
    }

    @Override
    public synchronized int getCurrentRound() {
        return this.currentRound;
    }

    @Override
    public synchronized List<Boolean> getRounds() {
        if (this.gameStart == null) throw new RuntimeException("Game must be started");
        return Collections.unmodifiableList(this.gameStart.rounds);
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
    public synchronized void registerSpectator(Spectator spectator) {
        this.spectators.add(spectator);
    }

    @Override
    public synchronized void unregisterSpectator(Spectator spectator) {
        this.spectators.remove(spectator);
    }

    @Override
    public synchronized Collection<Spectator> getSpectators() {
        return Collections.unmodifiableSet(this.spectators);
    }

}
