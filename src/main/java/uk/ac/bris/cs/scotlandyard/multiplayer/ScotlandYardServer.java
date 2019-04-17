package uk.ac.bris.cs.scotlandyard.multiplayer;

import com.google.gson.Gson;
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
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class ScotlandYardServer implements Spectator, ServerDelegate {

	//Creates a network and waits for it to start
	public static Future<ScotlandYardServer> CreateScotlandYardServer(ResourceManager manager, int port, int maxPlayers, Integer turnTimer, String serverName) {
		InetSocketAddress address = new InetSocketAddress("0.0.0.0", port);
		ScotlandYardServer server = new ScotlandYardServer(manager, address, maxPlayers, turnTimer, serverName);
		return server.startupFuture;
	}

	private Counter playerIDCounter = new Counter();
	private List<ServerPlayer> players = new ArrayList<>();


	private class ServerPlayer implements Player {
		String name;
		Colour colour;
		Boolean ready;
		WebSocket conn;
		Integer id;
		Consumer<Move> consumer;

		ServerPlayer(WebSocket conn, String name) {
			this.conn = conn;
			this.name = name;
			this.colour = null;
			this.ready = false;
			this.id = ScotlandYardServer.this.playerIDCounter.next();
		}

		@Override
		public void makeMove(ScotlandYardView view, int location, Set<Move> moves, Consumer<Move> callback) {
			Notification otherUsers = new Notification(NotificationNames.MOVE_REQUEST.toString());
			Notification thisUser = new Notification(NotificationNames.MOVE_REQUEST.toString());

			MoveRequest request = new MoveRequest(this.colour);
			if (ScotlandYardServer.this.turnTimer != null) {

				request.deadline = Instant.now().plusSeconds(ScotlandYardServer.this.turnTimer);

				ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(new DaemonExecutorThreadFactory());
				ScheduledFuture<?> future = executor.schedule(() -> {
					ScotlandYardServer.this.handlePlayerExitOrTimeout(ServerPlayer.this);
				}, ScotlandYardServer.this.turnTimer, TimeUnit.SECONDS);

				this.consumer = m -> {
					future.cancel(false);
					this.consumer = null;
					callback.accept(m);
				};
			} else {
				this.consumer = m -> {
					this.consumer = null;
					callback.accept(m);
				};
			}
			otherUsers.content = gson.toJson(request);
			request.setMoves(moves);
			request.currentLocation = location;
			thisUser.content = gson.toJson(request);

			this.conn.send(gson.toJson(thisUser));
			ScotlandYardServer.this.players.stream().filter(p -> p != this).forEach(p -> p.conn.send(gson.toJson(otherUsers)));
		}
	}

	static String protocolVersionString = "1.0";
	private ScotlandYardGame model = null;
	private int maxPlayers;
	private Integer turnTimer;
	private String serverName;
	private ResourceManager manager;
	private Server server;
	private Gson gson = new Gson();
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
		ServerPlayer player = new ServerPlayer(conn, clientHandshake.getFieldValue("Username"));
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
		ServerPlayer player = this.players.stream().filter(p -> p.conn == conn).findFirst().orElse(null);
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
					Colour colour = ((request.data == null) ? null : Colour.valueOf(request.data));
					if (colour != null && this.players.stream().anyMatch(p -> p.colour == colour)){
						response.error = "Colour taken";
					} else {
						player.colour = colour;
						this.updateStartTime();
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
				Object object = StringSerializer.deserializeObject(request.data);
				if (object instanceof Move) {
					try {
						if (player.consumer != null) {
							player.consumer.accept((Move) object);
							response.data = "Success";
						} else {
							response.data = "Illegal operation";
						}
					} catch (IllegalArgumentException e) {
						response.error = e.getMessage();		//Illegal move
					}
				} else {
					response.error = "Illegal value";
				}
				break;
			case GET_TICKETS:
				TicketRequest get = this.gson.fromJson(request.data, TicketRequest.class);
				if (get.ticket != null && get.colour != null) {
					response.data = this.model.getPlayerTickets(get.colour, get.ticket).orElse(0).toString();
				} else {
					response.data = "Illegal value";
				}
				break;
			case GET_LOCATION:
				try {
					Colour colour = Colour.valueOf(request.data);
					response.data = this.model.getPlayerLocation(colour).orElse(0).toString();
				} catch (IllegalArgumentException e) {
					response.error = "Illegal value";
				}
				break;
		}
	}

	@Override
	public void serverClientDisconnected(Server s, WebSocket conn) {
		this.players.stream().filter(p -> p.conn == conn).findFirst().ifPresent(player -> {
			this.players.remove(player);
			if (this.model == null) {					//If during setup phase
				this.updateStartTime();
				this.sendLobbyUpdateToAll();
			} else {
				this.handlePlayerExitOrTimeout(player);				//otherwise the other team wins
			}
		});
	}

	private void handlePlayerExitOrTimeout(ServerPlayer player) {
		Set<Colour> winners = new HashSet<>();
		this.model.getPlayers().forEach(p -> {
			if (p.isMrX() != player.colour.isMrX()) winners.add(p);
		});
		this.onGameOver(this.model, winners);
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
		}).collect(Collectors.toList());
		return lobby;
	}


	//Use a daemon thread so it doesn't stick around forever
	private ScheduledExecutorService gameExecutor = Executors.newSingleThreadScheduledExecutor(new DaemonExecutorThreadFactory());
	private ScheduledFuture<?> startFuture;
	private Instant startTime;

	private void updateStartTime() {
		if (this.model != null) return;
		if (this.players.stream().allMatch(p -> p.ready) && this.players.stream().filter(p -> p.colour != null ).count() >= 2
				&& this.players.stream().anyMatch(p -> p.colour == Colour.BLACK)) {
			int startDelay = 10;
			this.startTime = Instant.now().plusSeconds(startDelay);
			this.startFuture = gameExecutor.schedule(this::startGame, startDelay, TimeUnit.SECONDS);
		} else {
			if (this.startFuture != null) this.startFuture.cancel(false);
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
		MoveMade moveMade = new MoveMade(move, view.getCurrentPlayer());
		notification.content = gson.toJson(moveMade);
		this.sendNotificationToAll(notification);
	}

	public void onRoundStarted(ScotlandYardView view, int round) {
		Notification notification = new Notification(NotificationNames.ROUND_STARTED.toString());
		notification.content = String.valueOf(round);
		this.sendNotificationToAll(notification);
	}

	public void onGameOver(ScotlandYardView view, Set<Colour> winningPlayers) {
		Notification notification = new Notification(NotificationNames.GAME_OVER.toString());
		GameOver gameOver = new GameOver();
		gameOver.winningPlayers = winningPlayers;
		notification.content = gson.toJson(gameOver);
		this.sendNotificationToAll(notification);
	}

	@SuppressWarnings("Duplicates")		//PlayerConfiguration code is copied from LocalGame
	private void startGame() {

		//Find enabled colours and create ModelProperty
		Set<Colour> enabledColours = this.players.stream().map(p -> p.colour).collect(Collectors.toSet());
		ModelProperty setup = ModelProperty.createDefault(this.manager);
		setup.allPlayers().forEach(p -> p.enabledProperty().setValue(enabledColours.contains(p.colour())));
		this.randomisePlayerLocations(setup);

		List<PlayerConfiguration> configs = setup.players().stream()
				.map(p -> new PlayerConfiguration.Builder(p.colour())
						.at(p.location())
						.with(p.ticketsAsMap())
						.using(this.players.stream().filter(k -> k.colour == p.colour()).findFirst().orElseThrow(AssertionError::new))
						.build())
				.collect(Collectors.toList());

		PlayerConfiguration mrX = configs.stream().filter(p -> p.colour.isMrX()).findFirst().orElseThrow(AssertionError::new);
		List<PlayerConfiguration> detectives = configs.stream().filter(p -> p.colour.isDetective()).collect(Collectors.toList());

		this.model = new ScotlandYardModel(setup.revealRounds(), setup.graphProperty().get(), mrX, detectives.get(0), detectives.stream().skip(1).toArray(PlayerConfiguration[]::new));

		Notification notification = new Notification(NotificationNames.GAME_START.toString());
		GameStart gameStart = new GameStart();
		gameStart.players = model.getPlayers().stream().map(GameStartPlayer::new).collect(Collectors.toList());
		gameStart.players.forEach(gsp -> {
			ServerPlayer sp = this.players.stream().filter(p -> p.colour == gsp.colour).findFirst().orElseThrow(AssertionError::new);
			gsp.playerID = sp.id;
			gsp.username = sp.name;
			gsp.startLocation = this.model.getPlayerLocation(gsp.colour).orElseThrow(AssertionError::new);
		});
		gameStart.rounds = model.getRounds();
		notification.content = gson.toJson(gameStart);
		this.sendNotificationToAll(notification);

		model.registerSpectator(this);
		model.startRotate();
	}

	@SuppressWarnings("Duplicates")		//Copied from GameSetup
	private void randomisePlayerLocations(ModelProperty property) {
		ObservableList<PlayerProperty> players = property.players();
		ArrayList<Integer> availableLocation = new ArrayList<>(StandardGame.DETECTIVE_LOCATIONS);
		Collections.shuffle(availableLocation);
		ArrayDeque<Integer> deque = new ArrayDeque<>(availableLocation);
		players.forEach(p -> p.locationProperty().unbind());
		players.filtered(PlayerProperty::randomLocation).forEach(p -> {
			if (p.mrX()) {
				p.locationProperty().set(StandardGame.MRX_LOCATIONS
						.get(new Random().nextInt(StandardGame.MRX_LOCATIONS.size())));
			} else {
				p.locationProperty().set(deque.pop());
			}
		});
	}

	public void close() {
		try {
			this.server.stop(0);
			System.out.println("ScotlandYardServer closed");
		} catch (InterruptedException ignored) {
		}
	}

}
