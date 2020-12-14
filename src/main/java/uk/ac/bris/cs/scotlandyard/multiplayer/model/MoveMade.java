package uk.ac.bris.cs.scotlandyard.multiplayer.model;

import uk.ac.bris.cs.scotlandyard.model.Colour;
import uk.ac.bris.cs.scotlandyard.model.Move;

public class MoveMade {

    private String serializedMove;
    public Colour currentPlayer;

    public MoveMade(Move move, Colour currentPlayer) {
        this.serializedMove = StringSerializer.serializeObject(move);
        this.currentPlayer = currentPlayer;
    }

    public Move getMove() {
        return (Move) StringSerializer.deserializeObject(this.serializedMove);
    }

}
