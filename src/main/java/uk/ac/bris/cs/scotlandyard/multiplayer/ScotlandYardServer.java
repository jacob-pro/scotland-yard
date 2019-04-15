package uk.ac.bris.cs.scotlandyard.multiplayer;

import com.google.gson.Gson;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import uk.ac.bris.cs.scotlandyard.ResourceManager;
import uk.ac.bris.cs.scotlandyard.model.*;
import uk.ac.bris.cs.scotlandyard.multiplayer.network.*;
import uk.ac.bris.cs.scotlandyard.multiplayer.model.*;
import uk.ac.bris.cs.scotlandyard.ui.model.ModelProperty;
import uk.ac.bris.cs.scotlandyard.ui.model.PlayerProperty;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

public class ScotlandYardServer implements Spectator, Player, ServerDelegate {

	//Creates a network and waits for it to start
	public static Future<ScotlandYardServer> CreateScotlandYardServer(ResourceManager manager, InetSocketAddress address, int maxPlayers, Integer turnTimer, String serverName) {
		ScotlandYardServer server = new ScotlandYardServer(manager, address, maxPlayers, turnTimer, serverName);
		return server.startupFuture;
	}

	private Counter playerIDCounter = new Counter();
	private class Player {
		String name;
		Colour colour;
		Boolean ready;
		WebSocket conn;
		Integer id;

		Player(WebSocket conn, String name) {
			this.conn = conn;
			this.name = name;
			this.colour = null;
			this.ready = false;
			this.id = ScotlandYardServer.this.playerIDCounter.next();
		}
	}

	static String protocolVersionString = "1.0";
	static String undecidedColour = "null";
	private ScotlandYardGame model = null;
	private int maxPlayers;
	private Integer turnTimer;
	private String serverName;
	private ResourceManager manager;
	private Server server;
	private Date startTime;
	private Timer timer;
	private Gson gson = new Gson();
	private List<Player> players = new ArrayList<>();
	private CompletableFuture<ScotlandYardServer> startupFuture = new CompletableFuture<>();

	private ScotlandYardServer(ResourceManager manager, InetSocketAddress address, int maxPlayers, Integer turnTimer, String serverName) {
		this.manager = manager;
		this.maxPlayers = maxPlayers;
		this.turnTimer = turnTimer;
		this.serverName = serverName;
		this.server = new Server(address, this);
		this.server.start();
	}

	@Override
	public void serverDidStart(Server s, Exception e) {
		if (e == null) {
			this.startupFuture.complete(this);
		} else {
			this.startupFuture.completeExceptionally(e);
		}
	}

	@Override
	public String serverReceivedConnection(Server s, ClientHandshake clientHandshake, WebSocket conn) throws ServerJoinException {
		//Check that the player is able to join
		if (this.model != null) throw new ServerJoinException(JoinErrors.GAME_STARTED.toString());
		if (this.players.size() >= this.maxPlayers) throw new ServerJoinException(JoinErrors.SERVER_FULL.toString());
		if (!clientHandshake.getFieldValue("Version").equals(ScotlandYardServer.protocolVersionString))
			throw new ServerJoinException(JoinErrors.VERSION_MISMATCH.toString());

		//Create the player
		Player player = new Player(conn, clientHandshake.getFieldValue("Username"));
		this.players.add(player);

		//Fill the join message
		Join join = new Join();
		join.serverName = ScotlandYardServer.this.serverName;
		join.maxPlayers = ScotlandYardServer.this.maxPlayers;
		join.turnTimer = ScotlandYardServer.this.turnTimer;
		join.playerID = player.id;

		//Notify players that someone is joining
		this.sendLobbyUpdateToAll();
		return gson.toJson(join);
	}

	@Override
	public void serverReceivedRequest(Server s, Request request, Response response, WebSocket conn) {
		Player player = this.players.stream().filter(p -> p.conn == conn).findFirst().orElse(null);
		if (player == null) {
			response.error = "Unknown player";
			return;
		}
		RequestActions action = Arrays.stream(RequestActions.values()).filter(v -> v.toString().equals(request.action)).findAny().orElse(null);
		if (action == null) {
			response.error = "Unknown action";
			return;
		}
		switch (action) {
			case GET_LOBBY:
				Lobby lobby = this.currentLobby();
				response.data = this.gson.toJson(lobby);
				break;
			case SET_COLOUR:
				try {
					Colour colour = (request.data.equals(undecidedColour) ? null : Colour.valueOf(request.data));
					if (colour != null && this.players.stream().anyMatch(p -> p.colour == colour)){
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
					this.updateStartTime();
					this.sendLobbyUpdateToAll();
				} catch (IllegalArgumentException e) {
					response.error = "Illegal value";
				}
				break;
			case MAKE_MOVE:
				break;
		}
	}

	@Override
	public void serverClientDisconnected(Server s, WebSocket conn) {
		Player player = this.players.stream().filter(p -> p.conn == conn).findFirst().orElse(null);
		if (player == null) return;
		this.players.remove(player);
		//If during setup phase
		if (this.model == null) {
			this.updateStartTime();
			this.sendLobbyUpdateToAll();
		} else {
			//the other team wins
		}
	}

	private Lobby currentLobby() {
		Lobby lobby = new Lobby();
		lobby.startTime = this.startTime;
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

	private void updateStartTime() {
		if (this.players.stream().allMatch(p -> p.ready) && this.players.stream().filter(p -> p.colour != null ).count() >= 2
				&& this.players.stream().anyMatch(p -> p.colour == Colour.BLACK)) {
			Calendar cal = Calendar.getInstance();
			cal.add(Calendar.SECOND, 11);
			this.startTime = cal.getTime();
			this.timer = new Timer();
			this.timer.schedule(
					new java.util.TimerTask() {
						@Override
						public void run() {
							startGame();
						}
					}, this.startTime
			);
		} else {
			if (this.timer != null) this.timer.cancel();
			this.startTime = null;
		}
	}

	private void sendLobbyUpdateToAll() {
		Notification notification = new Notification(NotificationNames.LOBBY_UPDATE.toString());
		notification.content = gson.toJson(ScotlandYardServer.this.currentLobby());
		this.sendNotificationToAll(notification);
	}

	private void sendNotificationToAll(Notification notification) {
		String s = this.gson.toJson(notification);
		this.players.forEach(p -> p.conn.send(s));
	}

	@Override
	public void onRotationComplete(ScotlandYardView view) {
		if (!view.isGameOver()) this.model.startRotate();
		Notification notification = new Notification(NotificationNames.ROTATION_COMPLETE.toString());
		this.sendNotificationToAll(notification);
	}

	public void onMoveMade(ScotlandYardView view, Move move) {
		Notification notification = new Notification(NotificationNames.MOVE_MADE.toString());
		this.sendNotificationToAll(notification);
	}

	public void onRoundStarted(ScotlandYardView view, int round) {
		Notification notification = new Notification(NotificationNames.ROUND_STARTED.toString());
		notification.content = String.valueOf(round);
		this.sendNotificationToAll(notification);
	}

	public void onGameOver(ScotlandYardView view, Set<Colour> winningPlayers) {
		Notification notification = new Notification(NotificationNames.GAME_OVER.toString());
		notification.content = gson.toJson(winningPlayers);
		this.sendNotificationToAll(notification);
	}

	@Override
	public void makeMove(ScotlandYardView view, int location, Set<Move> moves, Consumer<Move> callback) {
		Colour colour = view.getCurrentPlayer();
		Player player = this.players.stream().filter(p -> p.colour == colour).findFirst().orElseThrow();
		Notification notification = new Notification(NotificationNames.MOVE_REQUEST.toString());
	}


	// Code below is copied from GameSetup - Use random locations for all players
	@SuppressWarnings("Duplicates")
	private void startGame() {

		Set<Colour> enabledColours = this.players.stream().map(p -> p.colour).collect(Collectors.toSet());

		ModelProperty defaults = ModelProperty.createDefault(this.manager);
		ObservableList<PlayerProperty> enabledPlayers = FXCollections.observableArrayList(defaults.allPlayers().stream()
				.filter(p -> enabledColours.contains(p.colour())).collect(toList()));

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

		Notification notification = new Notification(NotificationNames.GAME_START.toString());
		this.sendNotificationToAll(notification);

		model.registerSpectator(this);
		model.startRotate();
	}

	public void close() {
		try {
			this.server.stop(0);
		} catch (InterruptedException ignored) {

		}
	}

}