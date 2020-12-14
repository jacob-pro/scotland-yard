package uk.ac.bris.cs.scotlandyard.multiplayer.model;

import uk.ac.bris.cs.scotlandyard.model.Colour;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Lobby {

    public List<LobbyPlayer> players;
    public Instant startTime;

    //List of colours that are not in use by any other players
    public List<Colour> availableColours() {
        List<Colour> available = new ArrayList<>(Arrays.asList(Colour.values()));
        available.removeAll(this.takenColours());
        return available;
    }

    //List of colours which are in use by other players
    public List<Colour> takenColours() {
        return this.players.stream().filter(p -> p.colour != null).map(p -> p.colour).collect(Collectors.toList());
    }
}
