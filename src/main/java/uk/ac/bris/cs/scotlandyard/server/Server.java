package uk.ac.bris.cs.scotlandyard.server;

import com.google.gson.Gson;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import uk.ac.bris.cs.scotlandyard.ResourceManager;
import uk.ac.bris.cs.scotlandyard.model.*;
import uk.ac.bris.cs.scotlandyard.server.messaging.*;
import uk.ac.bris.cs.scotlandyard.ui.model.ModelProperty;
import uk.ac.bris.cs.scotlandyard.ui.model.PlayerProperty;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

public class Server implements Spectator, Player {

	//Creates a server and waits for it to start
	public static Future<Server> CreateMLGServer(ResourceManager manager, InetSocketAddress address, int maxPlayers, Integer turnTimer, String serverName) {
		Server server = new Server(manager, address, maxPlayers, turnTimer, serverName);
		server.internal.start();
		return server.internal.creationFuture;
	}

	private class ServerPlayer {
		String name;
		Colour colour = null;
		Boolean ready = false;
		WebSocket conn;
		Integer id;
	}

	static String protocolVersionString = "1.0";
	private ScotlandYardGame model = null;
	private int maxPlayers;
	private Integer turnTimer;
	private String serverName;
	private ResourceManager manager;
	private MLGServerInternal internal;
	private Gson gson = new Gson();
	private List<ServerPlayer> players = new ArrayList<>();
	private Counter playerIDCounter = new Counter();

	private Server(ResourceManager manager, InetSocketAddress address, int maxPlayers, Integer turnTimer, String serverName) {
		this.manager = manager;
		this.maxPlayers = maxPlayers;
		this.turnTimer = turnTimer;
		this.serverName = serverName;
		this.internal = new MLGServerInternal(address);
	}

	private void handleRequest(Request request, ServerPlayer player) {
		Response response = new Response();
		response.streamID = request.streamID;
		if (request.action == null) {
			response.error = "Unknown action";
		} else {
			switch (request.action) {
				case GET_LOBBY:
					Lobby lobby = this.currentLobby();
					response.data = this.gson.toJson(lobby);
					break;
				case SET_COLOUR:
					try {
						Colour colour = Colour.valueOf(request.data);
						if (this.players.stream().anyMatch(p -> p.colour == colour)){
							response.error = "Colour taken";
						} else {
							player.colour = colour;
							this.sendLobbyUpdateToAll();
						}
					} catch (IllegalArgumentException e) {
						response.error = "Illegal colour";
					}
					break;
				case SET_READY:
					try {
						player.ready = Boolean.valueOf(request.data);
						response.data = "Success";
						this.sendLobbyUpdateToAll();
					} catch (IllegalArgumentException e) {
						response.error = "Illegal value";
					}
					break;
				case MAKE_MOVE:
					break;
			}
		}
		player.conn.send(this.gson.toJson(response));
	}

	private void handlePlayerExit(ServerPlayer player) {
		this.players.remove(player);
	}

	private Lobby currentLobby() {
		Lobby lobby = new Lobby();
		lobby.startTime = null;
		lobby.players = this.players.stream().map(p -> {
			LobbyPlayer player = new LobbyPlayer();
			player.colour = p.colour;
			player.ready = p.ready;
			player.name = p.name;
			player.id = p.id;
			return player;
		}).collect(toList());
		return lobby;
	}

	private void sendLobbyUpdateToAll() {
		Notification notification = new Notification(Notification.NotificationName.LOBBY_UPDATE);
		notification.content = gson.toJson(Server.this.currentLobby());
		this.sendNotificationToAll(notification);
	}

	private void sendNotificationToAll(Notification notification) {
		String s = this.gson.toJson(notification);
		this.players.forEach(p -> p.conn.send(s));
	}

	@Override
	public void onRotationComplete(ScotlandYardView view) {
		if (!view.isGameOver()) this.model.startRotate();
		Notification notification = new Notification(Notification.NotificationName.ROTATION_COMPLETE);
		this.sendNotificationToAll(notification);
	}

	public void onMoveMade(ScotlandYardView view, Move move) {
		Notification notification = new Notification(Notification.NotificationName.MOVE_MADE);
		this.sendNotificationToAll(notification);
	}

	public void onRoundStarted(ScotlandYardView view, int round) {
		Notification notification = new Notification(Notification.NotificationName.ROUND_STARTED);
		notification.content = String.valueOf(round);
		this.sendNotificationToAll(notification);
	}

	public void onGameOver(ScotlandYardView view, Set<Colour> winningPlayers) {
		Notification notification = new Notification(Notification.NotificationName.GAME_OVER);
		notification.content = gson.toJson(winningPlayers);
		this.sendNotificationToAll(notification);
	}

	@Override
	public void makeMove(ScotlandYardView view, int location, Set<Move> moves, Consumer<Move> callback) {
		Colour colour = view.getCurrentPlayer();
		@SuppressWarnings("OptionalGetWithoutIsPresent")
		ServerPlayer player = this.players.stream().filter(p -> p.colour == colour).findFirst().get();
		Notification notification = new Notification(Notification.NotificationName.MOVE_REQUEST);
	}

	private class MLGServerInternal extends WebSocketServer {

		private CompletableFuture<Server> creationFuture = new CompletableFuture<>();
		private MessageDeserializer messageDeserializer = new MessageDeserializer();

		MLGServerInternal(InetSocketAddress address) {
			super(address);
		}

		@Override
		public void onStart() {
			this.creationFuture.complete(Server.this);
		}

		@Override
		public void onOpen(WebSocket conn, ClientHandshake handshake) {

			//Create a JOIN message
			Join join = new Join();

			if (Server.this.model != null) {
				join.error = Join.Error.GAME_STARTED;
				conn.send(gson.toJson(join));
				conn.close();
			}
			if (Server.this.players.size() >= Server.this.maxPlayers) {
				join.error = Join.Error.SERVER_FULL;
				conn.send(gson.toJson(join));
				conn.close();
			}
			if (!handshake.getFieldValue("Version").equals(Server.protocolVersionString)) {
				join.error = Join.Error.VERSION_MISMATCH;
				conn.send(gson.toJson(join));
				conn.close();
			}

			//Create the player
			ServerPlayer player = new ServerPlayer();
			player.conn = conn;
			player.name = handshake.getFieldValue("Username");
			player.id = Server.this.playerIDCounter.next();
			Server.this.players.add(player);

			//Fill the join message
			join.serverName = Server.this.serverName;
			join.maxPlayers = Server.this.maxPlayers;
			join.turnTimer = Server.this.turnTimer;
			join.playerID = player.id;

			conn.send(gson.toJson(join));
			Server.this.sendLobbyUpdateToAll();
		}

		@Override
		public void onClose(WebSocket conn, int code, String reason, boolean remote) {
			if (remote) {
				Optional<ServerPlayer> player = Server.this.players.stream().filter(p -> p.conn == conn).findFirst();
				player.ifPresent(Server.this::handlePlayerExit);
			}
		}

		@Override
		public void onMessage(WebSocket conn, String string) {
			this.messageDeserializer.deserialize(string).ifPresent(m -> m.accept(new MessageVisitor() {
				//Incoming requests
				@Override
				public void accept(Request message) {
					Optional<ServerPlayer> player = Server.this.players.stream().filter(p -> p.conn == conn).findFirst();
					player.ifPresent(p -> Server.this.handleRequest(message, p));
				}
			}));
		}

		@Override
		public void onError(WebSocket conn, Exception ex) {
			// Note that if error is fatal onClose is also called
			if(!this.creationFuture.isDone()) {
				this.creationFuture.completeExceptionally(ex);
			}
		}
	}

	private void startGame() {

		Set<Colour> enabledColours = this.players.stream().map(p -> p.colour).collect(Collectors.toSet());

		ModelProperty defaults = ModelProperty.createDefault(this.manager);
		ObservableList<PlayerProperty> enabledPlayers = FXCollections.observableArrayList(defaults.allPlayers().stream()
				.filter(p -> enabledColours.contains(p.colour())).collect(toList()));

		// Code below is copied from GameSetup - Use random locations for all players
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

		Notification notification = new Notification(Notification.NotificationName.GAME_START);
		this.sendNotificationToAll(notification);

		model.registerSpectator(this);
		model.startRotate();
	}

}
