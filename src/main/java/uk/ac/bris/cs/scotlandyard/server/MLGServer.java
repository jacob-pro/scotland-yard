package uk.ac.bris.cs.scotlandyard.server;

import com.google.common.collect.ImmutableSet;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ToggleButton;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import uk.ac.bris.cs.gamekit.graph.ImmutableGraph;
import uk.ac.bris.cs.scotlandyard.ResourceManager;
import uk.ac.bris.cs.scotlandyard.model.*;
import uk.ac.bris.cs.scotlandyard.ui.controller.Board;
import uk.ac.bris.cs.scotlandyard.ui.controller.BoardPlayers;
import uk.ac.bris.cs.scotlandyard.ui.model.ModelProperty;
import uk.ac.bris.cs.scotlandyard.ui.model.PlayerProperty;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static io.atlassian.fugue.Option.fromOptional;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;

public class MLGServer implements Spectator, Player {

	//Creates a server and waits for it to start
	public static Future<MLGServer> CreateMLGServer(ResourceManager manager, InetSocketAddress address, int maxPlayers, int turnTimer, String serverName) {
		MLGServer server = new MLGServer(manager, address, maxPlayers, turnTimer, serverName);
		server.internal.start();
		return server.internal.creationFuture;
	}

	private ScotlandYardGame model = null;
	private int maxPlayers;
	private int turnTimer;
	private String serverName;
	private ResourceManager manager;
	private MLGServerInternal internal;

	private MLGServer(ResourceManager manager, InetSocketAddress address, int maxPlayers, int turnTimer, String serverName) {
		this.manager = manager;
		this.maxPlayers = maxPlayers;
		this.turnTimer = turnTimer;
		this.serverName = serverName;
		this.internal = new MLGServerInternal(address);
	}

	private HashMap<WebSocket, Colour> connectionMap = new HashMap<>();

	//Create the game model
	private void startGame() {

		ModelProperty setup = this.createGameConfig();

		List<PlayerConfiguration> configs = setup.players().stream()
				.map(p -> new PlayerConfiguration.Builder(p.colour())
						.at(p.location())
						.with(p.ticketsAsMap())
						.using(this)
						.build())
				.collect(Collectors.toList());

		PlayerConfiguration mrX = configs.stream().filter(p -> p.colour.isMrX()).findFirst().orElseThrow(AssertionError::new);
		List<PlayerConfiguration> detectives = configs.stream().filter(p -> p.colour.isDetective()).collect(toList());

		this.model = new ScotlandYardModel(setup.revealRounds(), setup.graphProperty().get(), mrX, detectives.get(0), detectives.stream().skip(1).toArray(PlayerConfiguration[]::new));
		model.registerSpectator(this);
		model.startRotate();
	}

	class MLGServerInternal extends WebSocketServer {

		private CompletableFuture<MLGServer> creationFuture = new CompletableFuture<>();

		MLGServerInternal(InetSocketAddress address) {
			super(address);
		}

		@Override
		public void onStart() {
			this.creationFuture.complete(MLGServer.this);
		}

		@Override
		public void onOpen(WebSocket conn, ClientHandshake handshake) {
			if (MLGServer.this.model != null) {
				conn.send("Game already started"); //This method sends a message to the new client
				conn.close();
			}
			conn.send("Welcome to "+ MLGServer.this.serverName);
		}

		@Override
		public void onClose(WebSocket conn, int code, String reason, boolean remote) {
			System.out.println("closed " + conn.getRemoteSocketAddress() + " with exit code " + code + " additional info: " + reason);
		}

		@Override
		public void onMessage(WebSocket conn, String message) {
			System.out.println("received message from "	+ conn.getRemoteSocketAddress() + ": " + message);
		}

		@Override
		public void onError(WebSocket conn, Exception ex) {
			if(!this.creationFuture.isDone()) {
				this.creationFuture.completeExceptionally(ex);
			}
			System.err.println("an error occured on connection " + conn.getRemoteSocketAddress()  + ":" + ex);
		}
	}

	@Override
	public void onRotationComplete(ScotlandYardView view) {
		if (!view.isGameOver()) this.model.startRotate();
	}

	public void onMoveMade(ScotlandYardView view, Move move) {

	}

	public void onRoundStarted(ScotlandYardView view, int round) {

	}

	public void onGameOver(ScotlandYardView view, Set<Colour> winningPlayers) {

	}

	@Override
	public void makeMove(ScotlandYardView view, int location, Set<Move> moves, Consumer<Move> callback) {

	}


	private ModelProperty createGameConfig() {

		ModelProperty defaults = ModelProperty.createDefault(this.manager);
		ObservableList<PlayerProperty> enabledPlayers = FXCollections.observableArrayList(defaults.allPlayers().stream().filter(p -> {
			return this.connectionMap.containsValue(p.colour());
		}).collect(toList()));

		// Copied from GameSetup - Use random locations for all players
		ArrayList<Integer> availableLocation = new ArrayList<>(StandardGame.DETECTIVE_LOCATIONS);
		Collections.shuffle(availableLocation);
		ArrayDeque<Integer> deque = new ArrayDeque<>(availableLocation);
		enabledPlayers.forEach(p -> p.locationProperty().unbind());
		enabledPlayers.filtered(PlayerProperty::randomLocation).forEach(p -> {
			if (p.mrX()) {
				p.locationProperty().set(StandardGame.MRX_LOCATIONS
						.get(new Random().nextInt(StandardGame.MRX_LOCATIONS.size())));
			} else {
				p.locationProperty().set(deque.pop());
			}
		});

		return new ModelProperty(null, defaults.revealRounds(), enabledPlayers, defaults.graphProperty().get());
	}

}
