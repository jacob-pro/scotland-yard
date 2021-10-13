package uk.ac.bris.cs.scotlandyard.multiplayer.model;

import uk.ac.bris.cs.scotlandyard.model.Colour;
import uk.ac.bris.cs.scotlandyard.model.Move;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

public class MoveRequest {

    //These will be populated regardless or which player is due to move
    public Instant deadline;        //Null if there is no turn timer
    public Colour colour;

    //These will only be populated if it is for us
    private Set<String> serializedMoves;
    public Integer currentLocation;

    public MoveRequest(Colour colour) {
        this.colour = colour;
    }

    public void setMoves(Set<Move> moves) {
        this.serializedMoves = new HashSet<>();
        moves.forEach(m -> this.serializedMoves.add(StringSerializer.serializeObject(m)));
    }

    public Set<Move> getMoves() {
        Set<Move> moves = new HashSet<>();
        this.serializedMoves.forEach(s -> {
            Move move = (Move) StringSerializer.deserializeObject(s);
            moves.add(move);
        });
        return moves;
    }

}
