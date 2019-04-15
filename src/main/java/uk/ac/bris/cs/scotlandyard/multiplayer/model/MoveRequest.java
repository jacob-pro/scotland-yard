package uk.ac.bris.cs.scotlandyard.multiplayer.model;

import uk.ac.bris.cs.scotlandyard.model.Colour;
import uk.ac.bris.cs.scotlandyard.model.Move;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public class MoveRequest {

	public Date deadline;
	public Colour colour;
	private Set<String> serializedMoves;
	public Integer currentLocation;

	public MoveRequest(Colour colour) {
		this.colour = colour;
	}

	public void setMoves(Set<Move> moves) {
		this.serializedMoves = new HashSet<>();
		moves.forEach(m -> {
			this.serializedMoves.add(StringSerializer.serializeObject(m));
		});
	}

	public Set<Move> getMoves() {
		Set<Move> moves = new HashSet<>();
		if (this.serializedMoves != null) {
			this.serializedMoves.forEach(s -> {
				Move move = (Move) StringSerializer.deserializeObject(s);
				moves.add(move);
			});
		}
		return moves;
	}

}
