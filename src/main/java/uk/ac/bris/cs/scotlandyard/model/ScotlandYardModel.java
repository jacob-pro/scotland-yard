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
	private int currentRound = NOT_STARTED;

	private void validateTicketsMap(Map<Ticket, Integer> map) {
	    for (Ticket ticket: Ticket.values()) {
	        if (!map.containsKey(ticket)) {
	            throw new IllegalArgumentException("Tickets map must be populated with values for all tickets");
            }
        }
    }

	private List<PlayerConfiguration> validatePlayerConfigurations(PlayerConfiguration mrX,
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

		return playerConfigurations;
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
		return this.playerConfigurations.stream().map(c -> c.colour).collect(Collectors.toUnmodifiableList());
	}

	@Override
	public Set<Colour> getWinningPlayers() {
		// TODO
		throw new RuntimeException("Implement me");
	}

	@Override
	public Optional<Integer> getPlayerLocation(Colour colour) {
	    Optional<PlayerConfiguration> cfg = this.playerConfigurations.stream().filter(c -> c.colour == colour).findFirst();
	    return cfg.map(c -> c.location);
	}

	@Override
	public Optional<Integer> getPlayerTickets(Colour colour, Ticket ticket) {
        Optional<PlayerConfiguration> cfg = this.playerConfigurations.stream().filter(c -> c.colour == colour).findFirst();
        Optional<Map<Ticket, Integer>> tickets = cfg.map(c -> c.tickets);
        return tickets.map(t -> t.get(ticket));
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
