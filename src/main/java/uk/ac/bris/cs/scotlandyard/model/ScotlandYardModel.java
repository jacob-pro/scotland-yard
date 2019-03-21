package uk.ac.bris.cs.scotlandyard.model;

import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import uk.ac.bris.cs.gamekit.graph.Edge;
import uk.ac.bris.cs.gamekit.graph.Graph;
import uk.ac.bris.cs.gamekit.graph.Node;
import uk.ac.bris.cs.gamekit.graph.ImmutableGraph;

// TODO implement all methods and pass all tests
public class ScotlandYardModel implements ScotlandYardGame {

	private Collection<Spectator> spectators = new HashSet<>();
	private List<Boolean> rounds;
	private Graph<Integer, Transport> graph;
	private List<ScotlandYardPlayer> players;
	private Set<ScotlandYardPlayer> winningPlayers = new HashSet<>();
	private int currentRound = NOT_STARTED;

	private void validateTicketsMap(Map<Ticket, Integer> map) {
		for (Ticket ticket: Ticket.values()) {
			if (!map.containsKey(ticket)) {
				throw new IllegalArgumentException("Tickets map must be populated with values for all tickets");
			}
		}
	}

	private List<ScotlandYardPlayer> createPlayers(PlayerConfiguration mrX,
													PlayerConfiguration firstDetective,
													PlayerConfiguration... restOfTheDetectives) {

		//Check MrX exists and is not null
		Objects.requireNonNull(mrX,"MrX must not be null");
		if (!mrX.colour.isMrX()) throw new IllegalArgumentException("First player must be mrX");

		//Assemble a list of player configurations
		List<PlayerConfiguration> playerConfigurations = new ArrayList<>();
		playerConfigurations.add(mrX);
		playerConfigurations.add(Objects.requireNonNull(firstDetective, "First detective must not be null"));
		for (PlayerConfiguration config: restOfTheDetectives){
			playerConfigurations.add(Objects.requireNonNull(config, "restOfTheDetectives must not be null"));
		}

		//Validate the player configurations
		Set<Colour> allColours = new HashSet<>();
		Set<Integer> allLocations = new HashSet<>();
		playerConfigurations.forEach(cfg -> {
			//Check colours
			if (allColours.contains(cfg.colour)) throw new IllegalArgumentException("Duplicate player colour");
			allColours.add(cfg.colour);
			//Check locations
			if (allLocations.contains(cfg.location)) throw new IllegalArgumentException("Duplicate player location");
			allLocations.add(cfg.location);
			// Check player ticket maps
			this.validateTicketsMap(cfg.tickets);
			// Check detective double tickets
			if (cfg.colour.isDetective()) {
				if (cfg.tickets.get(Ticket.SECRET) > 0) throw new IllegalArgumentException("Detectives must not have secret tickets");
				if (cfg.tickets.get(Ticket.DOUBLE) > 0) throw new IllegalArgumentException("Detectives must not have double tickets");
			}
		});

		return playerConfigurations.stream()
				.map(c -> new ScotlandYardPlayer(c.player, c.colour, c.location, c.tickets)).collect(Collectors.toList());
	}

	public ScotlandYardModel(List<Boolean> rounds, Graph<Integer, Transport> graph,
			PlayerConfiguration mrX, PlayerConfiguration firstDetective,
			PlayerConfiguration... restOfTheDetectives) {

		this.rounds = Objects.requireNonNull(rounds);
		this.graph = Objects.requireNonNull(graph);
		this.players = this.createPlayers(mrX, firstDetective, restOfTheDetectives);
	}

	@Override
	public void registerSpectator(Spectator spectator) {
		Objects.requireNonNull(spectator, "Spectator must not be null");
		if (this.spectators.contains(spectator)) throw new IllegalArgumentException("Spectator already registered");
		this.spectators.add(spectator);
	}

	@Override
	public void unregisterSpectator(Spectator spectator) {
		Objects.requireNonNull(spectator, "Spectator must not be null");
		if (!this.spectators.remove(spectator)) {
			throw new IllegalArgumentException("Can't unregister a spectator that has never been registered before");
		}
	}

	private Set<TicketMove> generateTicketMovesForPlayerFromLocation(ScotlandYardPlayer player, Integer location) {
		Set<TicketMove> ticketMoves = new HashSet<>();
		Node<Integer> currentNode = this.graph.getNode(location);
		Collection<Edge<Integer, Transport>> edgesAtLocation = this.graph.getEdgesFrom(currentNode);

		edgesAtLocation.forEach(edge -> {
			int destination = edge.destination().value();
			boolean destinationBlocked = this.players.stream().anyMatch(p -> p.location() == destination && p.isDetective());

			if (!destinationBlocked) {

				Ticket requiredTicketType = Ticket.fromTransport(edge.data());
				if (player.hasTickets(requiredTicketType)) {
					ticketMoves.add(new TicketMove(player.colour(), requiredTicketType, edge.destination().value()));
				}
				if (player.hasTickets(requiredTicketType)) {
					ticketMoves.add(new TicketMove(player.colour(), Ticket.SECRET, edge.destination().value()));
				}
			}
		});

		return ticketMoves;
	}

	private Set<Move> validMovesForPlayer(ScotlandYardPlayer player) {

		Set<TicketMove> firsts = this.generateTicketMovesForPlayerFromLocation(player, player.location());
		Set<Move> moves = new HashSet<>(firsts);

		if(player.hasTickets(Ticket.DOUBLE)) {
			firsts.forEach(potentialFirst -> {
				player.removeTicket(potentialFirst.ticket());
				moves.addAll(this.generateTicketMovesForPlayerFromLocation(player, potentialFirst.destination()).stream()
						.map(secondMove -> new DoubleMove(player.colour(), potentialFirst, secondMove)).collect(Collectors.toSet()));
				player.addTicket(potentialFirst.ticket());
			});
		}

		if (moves.isEmpty() && player.isDetective()) {
			moves.add(new PassMove(player.colour()));
		}

		return moves;
	}

	private void makeMove(Set<Move> validMoves, Move move) {

	}

	//An explanation of the Consumer callback
	//We could create an inner class that implements the accept method
	//The accept method is a method with 1 parameter matching the generic type of the consumer
	//A Consumer is a functional interface, meaning that it only has one abstract (non default) method
	//A functional interface can be passed as as a lambda expression that matches the signature of its single method

	@Override
	public void startRotate() {
		ScotlandYardPlayer currentPlayer = this.players.get(0);
		Set<Move> validMoves = this.validMovesForPlayer(currentPlayer);
		currentPlayer.player().makeMove(this, currentPlayer.location(), validMoves, move -> this.makeMove(validMoves, move));
		Collections.rotate(this.players, 1);
	}

	@Override
	public Collection<Spectator> getSpectators() {
		return Collections.unmodifiableCollection(this.spectators);
	}

	@Override
	public List<Colour> getPlayers() {
		return this.players.stream().map(ScotlandYardPlayer::colour).collect(Collectors.toUnmodifiableList());
	}

	@Override
	public Set<Colour> getWinningPlayers() {
		return this.winningPlayers.stream().map(ScotlandYardPlayer::colour).collect(Collectors.toUnmodifiableSet());
	}

	@Override
	public Optional<Integer> getPlayerLocation(Colour colour) {
		Optional<ScotlandYardPlayer> cfg = this.players.stream().filter(c -> c.colour() == colour).findFirst();
		return cfg.map(ScotlandYardPlayer::location);
	}

	@Override
	public Optional<Integer> getPlayerTickets(Colour colour, Ticket ticket) {
		Optional<ScotlandYardPlayer> cfg = this.players.stream().filter(c -> c.colour() == colour).findFirst();
		Optional<Map<Ticket, Integer>> tickets = cfg.map(ScotlandYardPlayer::tickets);
		return tickets.map(t -> t.get(ticket));
	}
	//i'm helping

	@Override
	public boolean isGameOver() {
		return !this.winningPlayers.isEmpty();
	}

	@Override
	public Colour getCurrentPlayer() {
		return this.players.get(0).colour();
	}

	@Override
	public int getCurrentRound() {
		return this.currentRound;
	}

	@Override
	public List<Boolean> getRounds() {
		return Collections.unmodifiableList(this.rounds);
	}

	@Override
	public Graph<Integer, Transport> getGraph() {
		return new ImmutableGraph<>(this.graph);
	}

}
