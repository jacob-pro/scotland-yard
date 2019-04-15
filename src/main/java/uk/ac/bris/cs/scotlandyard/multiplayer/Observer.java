package uk.ac.bris.cs.scotlandyard.multiplayer;

import uk.ac.bris.cs.scotlandyard.model.Spectator;
import uk.ac.bris.cs.scotlandyard.multiplayer.model.Lobby;

public interface Observer extends Spectator {

	//All the calls will be made from the JavaFX main thread to simulate the standard model behaviour

	//When the game lobby changes - e.g. new player, changed colour, changed ready state
	default void onLobbyChange(Lobby lobby) {}

	//When the game starts
	default void onGameStarted() {}

	//When a player is requested to make a move
	default void onMoveRequested() {}

	//When something is wrong with the connection
	default void onConnectionError(ConnectionException e) {}

}
