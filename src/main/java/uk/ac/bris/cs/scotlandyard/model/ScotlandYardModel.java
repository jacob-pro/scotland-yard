package uk.ac.bris.cs.scotlandyard.model;

import java.util.*;
import java.util.stream.Collectors;

import uk.ac.bris.cs.gamekit.graph.Graph;
import uk.ac.bris.cs.gamekit.graph.ImmutableGraph;

// TODO implement all methods and pass all tests
public class ScotlandYardModel implements ScotlandYardGame {

	private Collection<Spectator> spectators = new HashSet<>();
	private List<Boolean> rounds;
	private Graph<Integer, Transport> graph;
	private List<PlayerConfiguration> playerConfigurations;

	private List<PlayerConfiguration> validatePlayerConfigurations(PlayerConfiguration mrX,
																   PlayerConfiguration firstDetective,
																   PlayerConfiguration... restOfTheDetectives) {

		Objects.requireNonNull(mrX,"MrX must not be null");
		if (!mrX.colour.isMrX()) throw new IllegalArgumentException("First player must be mrX");

		List<PlayerConfiguration> players = new ArrayList<>();
		players.add(mrX);
		players.add(Objects.requireNonNull(firstDetective, "First detective must not be null"));
		for (PlayerConfiguration config: restOfTheDetectives){
			players.add(Objects.requireNonNull(config, "restOfTheDetectives must not be null"));
		}

		Set<Colour> allColours = new HashSet<>();
		Set<Integer> allLocations = new HashSet<>();
		for (PlayerConfiguration player: players) {
			if (allColours.contains(player.colour)) throw new IllegalArgumentException("Duplicate player colour");
			allColours.add(player.colour);
			if (allLocations.contains(player.location)) throw new IllegalArgumentException("Duplicate player location");
			allLocations.add(player.location);
		}

		return players;
	}

	public ScotlandYardModel(List<Boolean> rounds, Graph<Integer, Transport> graph,
			PlayerConfiguration mrX, PlayerConfiguration firstDetective,
			PlayerConfiguration... restOfTheDetectives) {

		this.rounds = Objects.requireNonNull(rounds);
		this.graph = Objects.requireNonNull(graph);
		this.playerConfigurations = this.validatePlayerConfigurations(mrX, firstDetective, restOfTheDetectives);
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

	@Override
	public void startRotate() {
		// TODO
		throw new RuntimeException("Implement me");
	}

	@Override
	public Collection<Spectator> getSpectators() {
		return Collections.unmodifiableCollection(this.spectators);
	}

	@Override
	public List<Colour> getPlayers() {
		// Java is disgusting
		return this.playerConfigurations.stream().map(c -> c.colour).collect(Collectors.toUnmodifiableList());
	}

	@Override
	public Set<Colour> getWinningPlayers() {
		// TODO
		throw new RuntimeException("Implement me");
	}

	@Override
	public Optional<Integer> getPlayerLocation(Colour colour) {
		// TODO
		throw new RuntimeException("Implement me");
	}

	@Override
	public Optional<Integer> getPlayerTickets(Colour colour, Ticket ticket) {
		// TODO
		throw new RuntimeException("Implement me");
	}

	@Override
	public boolean isGameOver() {
		// TODO
		throw new RuntimeException("Implement me");
	}

	@Override
	public Colour getCurrentPlayer() {
		// TODO
		throw new RuntimeException("Implement me");
	}

	@Override
	public int getCurrentRound() {
		// TODO
		throw new RuntimeException("Implement me");
	}

	@Override
	public List<Boolean> getRounds() {
		// TODO
		throw new RuntimeException("Implement me");
	}

	@Override
	public Graph<Integer, Transport> getGraph() {
		return new ImmutableGraph<>(this.graph);
	}

}
