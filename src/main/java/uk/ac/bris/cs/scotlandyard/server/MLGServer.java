package uk.ac.bris.cs.scotlandyard.server;

import com.google.gson.Gson;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import uk.ac.bris.cs.scotlandyard.ResourceManager;
import uk.ac.bris.cs.scotlandyard.model.*;
import uk.ac.bris.cs.scotlandyard.server.messaging.Join;
import uk.ac.bris.cs.scotlandyard.server.messaging.Lobby;
import uk.ac.bris.cs.scotlandyard.server.messaging.LobbyPlayer;
import uk.ac.bris.cs.scotlandyard.ui.model.ModelProperty;
import uk.ac.bris.cs.scotlandyard.ui.model.PlayerProperty;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

public class MLGServer implements Spectator, Player {

	static Integer protocolVersion = 1;

	//Creates a server and waits for it to start
	public static Future<MLGServer> CreateMLGServer(ResourceManager manager, InetSocketAddress address, int maxPlayers, Integer turnTimer, String serverName) {
		MLGServer server = new MLGServer(manager, address, maxPlayers, turnTimer, serverName);
		server.internal.start();
		return server.internal.creationFuture;
	}

	private ScotlandYardGame model = null;
	private int maxPlayers;
	private Integer turnTimer;
	private String serverName;
	private ResourceManager manager;
	private MLGServerInternal internal;
	private Gson gson = new Gson();

	private MLGServer(ResourceManager manager, InetSocketAddress address, int maxPlayers, Integer turnTimer, String serverName) {
		this.manager = manager;
		this.maxPlayers = maxPlayers;
		this.turnTimer = turnTimer;
		this.serverName = serverName;
		this.internal = new MLGServerInternal(address);
	}

	private List<Player> players = new ArrayList<>();

	private class Player {
		String name;
		Colour colour = null;
		Boolean ready = false;
		WebSocket conn;
	}

	private class MLGServerInternal extends WebSocketServer {

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

			//Create a JOIN message
			Join join = new Join();

			if (MLGServer.this.model != null) {
				join.error = Join.Error.GAME_STARTED;
				conn.send(gson.toJson(join));
				conn.close();
			}
			if (MLGServer.this.players.size() >= MLGServer.this.maxPlayers) {
				join.error = Join.Error.SERVER_FULL;
				conn.send(gson.toJson(join));
				conn.close();
			}

			//Create the player
			Player player = new Player();
			player.conn = conn;
			player.name = handshake.getFieldValue("Username");
			MLGServer.this.players.add(player);

			//Fill the join message
			join.serverName = MLGServer.this.serverName;
			join.maxPlayers = MLGServer.this.maxPlayers;
			join.turnTimer = MLGServer.this.turnTimer;
			join.lobby = MLGServer.this.currentLobby(conn);

			conn.send(gson.toJson(join));
		}

		@Override
		public void onClose(WebSocket conn, int code, String reason, boolean remote) {
			if (remote) {
				Optional<Player> player = MLGServer.this.players.stream().filter(p -> {return p.conn == conn;}).findFirst();
				player.ifPresent(p -> {
					MLGServer.this.players.remove(p);
				});
				System.out.println("closed " + conn.getRemoteSocketAddress() + " with exit code " + code + " additional info: " + reason);
			}
		}

		@Override
		public void onMessage(WebSocket conn, String message) {
			System.out.println("received message from "	+ conn.getRemoteSocketAddress() + ": " + message);
		}

		@Override
		public void onError(WebSocket conn, Exception ex) {
			// Note that if error is fatal onClose is also called
			if(!this.creationFuture.isDone()) {
				this.creationFuture.completeExceptionally(ex);
			}
			System.err.println("an error occured on connection " + conn.getRemoteSocketAddress()  + ":" + ex);
		}

	}

	private Lobby currentLobby(WebSocket forConn) {
		Lobby lobby = new Lobby();
		lobby.startTime = null;
		lobby.players = this.players.stream().map(p -> {
			LobbyPlayer player = new LobbyPlayer();
			player.colour = p.colour;
			player.ready = p.ready;
			player.name = p.name;
			player.you = (p.conn == forConn);
			return player;
		}).collect(toList());
		return lobby;
	}

	private void sendToAll(String s) {
		this.players.forEach(p -> {
			p.conn.send(s);
		});
	}

	@Override
	public void onRotationComplete(ScotlandYardView view) {
		if (!view.isGameOver()) this.model.startRotate();
		this.sendToAll("rotation complete");
	}

	public void onMoveMade(ScotlandYardView view, Move move) {
		this.sendToAll("move made");
	}

	public void onRoundStarted(ScotlandYardView view, int round) {
		this.sendToAll("round started");
	}

	public void onGameOver(ScotlandYardView view, Set<Colour> winningPlayers) {
		this.sendToAll("game over");
	}

	@Override
	public void makeMove(ScotlandYardView view, int location, Set<Move> moves, Consumer<Move> callback) {
		Colour player = view.getCurrentPlayer();
	}

	private void startGame() {

		Set<Colour> enabledColours = this.players.stream().map(p -> {return p.colour;}).collect(Collectors.toSet());

		ModelProperty defaults = ModelProperty.createDefault(this.manager);
		ObservableList<PlayerProperty> enabledPlayers = FXCollections.observableArrayList(defaults.allPlayers().stream()
				.filter(p -> enabledColours.contains(p.colour())).collect(toList()));

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

		ModelProperty setup = new ModelProperty(null, defaults.revealRounds(), enabledPlayers, defaults.graphProperty().get());

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

}
