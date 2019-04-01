package uk.ac.bris.cs.scotlandyard.model;

import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

import uk.ac.bris.cs.gamekit.graph.Edge;
import uk.ac.bris.cs.gamekit.graph.Graph;
import uk.ac.bris.cs.gamekit.graph.Node;
import uk.ac.bris.cs.gamekit.graph.ImmutableGraph;

// TODO implement all methods and pass all tests
public class ScotlandYardModel implements ScotlandYardGame {

	private Collection<Spectator> spectators = new HashSet<>();
	private List<Boolean> rounds;
	private Graph<Integer, Transport> graph;
	private ScotlandYardPlayer mrX;				//Reference to MrX
	private Set<ScotlandYardPlayer> detectives;	//Unordered references of the detectives
	private List<ScotlandYardPlayer> players;
	private Set<ScotlandYardPlayer> winningPlayers = new HashSet<>();
	private int currentRound = NOT_STARTED;
	private int lastKnownMrXLocation = 0;		//Hidden location is apparently 0

	private void validateTicketsMap(Map<Ticket, Integer> map) {
		for (Ticket ticket: Ticket.values()) {
			if (!map.containsKey(ticket)) {
				throw new IllegalArgumentException("Tickets map must be populated with values for all tickets");
			}
		}
	}

	private boolean listContainsDuplicates(List list) {
		return list.size() != list.stream().distinct().count();
	}

	private void populateWinningPlayers() {
		boolean mrXisNext = this.getCurrentPlayer().isMrX();
		//Detectives have win if they have caught MrX
		//Or if MrX is unable to move on his turn
		//MrX wins if rounds are complete
		//Or all detectives are stuck (i.e. can only PassMove)
		if (detectives.stream().anyMatch(d -> d.location() == this.mrX.location())) {
			this.winningPlayers.addAll(this.detectives);
		} else if (mrXisNext && this.validMovesForPlayer(this.mrX).isEmpty()) {
			this.winningPlayers.addAll(this.detectives);
		} else if ((this.remainingRounds() == 0) && (mrXisNext)) {
			this.winningPlayers.add(this.mrX);
		} else if (this.detectives.stream().flatMap(p -> this.validMovesForPlayer(p).stream()).allMatch(m -> m instanceof PassMove)) {
			this.winningPlayers.add(this.mrX);
		}
	}

	private void createPlayers(PlayerConfiguration mrX,
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
		if(listContainsDuplicates(playerConfigurations.stream().map(c -> c.colour).collect(Collectors.toList()))) throw new IllegalArgumentException("Duplicate player colour");
		if(listContainsDuplicates(playerConfigurations.stream().map(c -> c.location).collect(Collectors.toList()))) throw new IllegalArgumentException("Duplicate player colour");

		playerConfigurations.forEach(cfg -> {
			// Check player ticket maps
			this.validateTicketsMap(cfg.tickets);
			// Check detective double tickets
			if (cfg.colour.isDetective()) {
				if (cfg.tickets.get(Ticket.SECRET) > 0) throw new IllegalArgumentException("Detectives must not have secret tickets");
				if (cfg.tickets.get(Ticket.DOUBLE) > 0) throw new IllegalArgumentException("Detectives must not have double tickets");
			}
		});

		this.players = playerConfigurations.stream().map(c -> new ScotlandYardPlayer(c.player, c.colour, c.location, c.tickets)).collect(Collectors.toList());
		this.mrX = this.players.get(0);
		this.detectives = new HashSet<>(this.players.subList(1, this.players.size()));
	}

	public ScotlandYardModel(List<Boolean> rounds, Graph<Integer, Transport> graph,
			PlayerConfiguration mrX, PlayerConfiguration firstDetective,
			PlayerConfiguration... restOfTheDetectives) {

		this.rounds = Objects.requireNonNull(rounds, "Rounds must not be null");
		this.graph = Objects.requireNonNull(graph, "Graph must not be null");
		this.createPlayers(mrX, firstDetective, restOfTheDetectives);
		this.populateWinningPlayers();
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
				if (player.hasTickets(Ticket.SECRET)) {
					ticketMoves.add(new TicketMove(player.colour(), Ticket.SECRET, edge.destination().value()));
				}
			}
		});

		return ticketMoves;
	}

	private int remainingRounds() {
		return rounds.size() - this.currentRound;
	}

	private Set<Move> validMovesForPlayer(ScotlandYardPlayer player) {

		Set<TicketMove> firsts = this.generateTicketMovesForPlayerFromLocation(player, player.location());
		Set<Move> moves = new HashSet<>(firsts);

		if(player.hasTickets(Ticket.DOUBLE) && this.remainingRounds() >= 2) {
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

	private void makeMove(ScotlandYardPlayer player, Set<Move> validMoves, Move move) {
		Objects.requireNonNull(move, "Move must not be null");

		if (!validMoves.contains(move)) throw new IllegalArgumentException("Invalid move");

		//Rotate the players
		Collections.rotate(this.players, -1);

		//This is an anonymous class that implements the MoveVisitor interface
		move.visit(new MoveVisitor() {

			private void incrementRound() {
				ScotlandYardModel.this.currentRound++;
				ScotlandYardModel.this.spectators.forEach(s -> s.onRoundStarted(ScotlandYardModel.this, ScotlandYardModel.this.currentRound));
			}

			@Override
			public void visit(PassMove move) {
				ScotlandYardModel.this.spectators.forEach(s -> s.onMoveMade(ScotlandYardModel.this, move));
			}

			@Override
			public void visit(TicketMove move) {
				player.removeTicket(move.ticket());
				player.location(move.destination());
				if (player.isMrX()) {
					if (ScotlandYardModel.this.rounds.get(ScotlandYardModel.this.currentRound)) ScotlandYardModel.this.lastKnownMrXLocation = player.location();
					Move concealedMove = new TicketMove(move.colour(), move.ticket(), ScotlandYardModel.this.lastKnownMrXLocation);
					this.incrementRound();
					ScotlandYardModel.this.spectators.forEach(s -> s.onMoveMade(ScotlandYardModel.this, concealedMove));
				} else {
					ScotlandYardModel.this.mrX.addTicket(move.ticket());
					ScotlandYardModel.this.spectators.forEach(s -> s.onMoveMade(ScotlandYardModel.this, move));
				}
			}

			//Note that a DoubleMove implies this is MrX
			//We must notify the spectators first with the double move, and then again with the first and second moves
			@Override
			public void visit(DoubleMove move) {

				if (ScotlandYardModel.this.rounds.get(ScotlandYardModel.this.currentRound)) ScotlandYardModel.this.lastKnownMrXLocation = move.firstMove().destination();
				int firstMoveConcealed = ScotlandYardModel.this.lastKnownMrXLocation;
				if (ScotlandYardModel.this.rounds.get(ScotlandYardModel.this.currentRound + 1)) ScotlandYardModel.this.lastKnownMrXLocation = move.secondMove().destination();
				int secondMoveConcealed = ScotlandYardModel.this.lastKnownMrXLocation;
				DoubleMove concealedMove = new DoubleMove(move.colour(), move.firstMove().ticket(), firstMoveConcealed, move.secondMove().ticket(), secondMoveConcealed);

				player.removeTicket(Ticket.DOUBLE);
				ScotlandYardModel.this.spectators.forEach(s -> s.onMoveMade(ScotlandYardModel.this, concealedMove));

				this.incrementRound();

				player.removeTicket(move.firstMove().ticket());
				player.location(move.firstMove().destination());
				ScotlandYardModel.this.spectators.forEach(s -> s.onMoveMade(ScotlandYardModel.this, concealedMove.firstMove()));

				this.incrementRound();

				player.removeTicket(move.secondMove().ticket());
				player.location(move.secondMove().destination());
				ScotlandYardModel.this.spectators.forEach(s -> s.onMoveMade(ScotlandYardModel.this, concealedMove.secondMove()));
			}
		});


		this.populateWinningPlayers();

		if (!this.winningPlayers.isEmpty()) {
			this.spectators.forEach(s -> s.onGameOver(this, this.getWinningPlayers()));
		} else if (this.getCurrentPlayer().isMrX()) {				//If the next player is now MrX then a rotation must have been completed
			this.spectators.forEach(s -> s.onRotationComplete(this));
		} else {
			this.startNextMove();
		}
	}

	//An explanation of the Consumer callback
	//We could create an inner class that implements the accept method
	//The accept method is a method with 1 parameter matching the generic type of the consumer
	//A Consumer is a functional interface, meaning that it only has one abstract (non default) method
	//A functional interface can be passed as as a lambda expression that matches the signature of its single method

	private void startNextMove() {
		ScotlandYardPlayer currentPlayer = this.players.get(0);
		Set<Move> validMoves = this.validMovesForPlayer(currentPlayer);
		currentPlayer.player().makeMove(this, currentPlayer.location(), validMoves, move -> this.makeMove(currentPlayer, validMoves, move));
	}

	@Override
	public void startRotate() {
		if (!this.winningPlayers.isEmpty()) throw new IllegalStateException("The game is over");
		this.startNextMove();
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
		return cfg.map(p -> p.isMrX() ? this.lastKnownMrXLocation : p.location());
	}

	@Override
	public Optional<Integer> getPlayerTickets(Colour colour, Ticket ticket) {
		Optional<ScotlandYardPlayer> cfg = this.players.stream().filter(c -> c.colour() == colour).findFirst();
		Optional<Map<Ticket, Integer>> tickets = cfg.map(ScotlandYardPlayer::tickets);
		return tickets.map(t -> t.get(ticket));
	}

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
