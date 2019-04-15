package uk.ac.bris.cs.scotlandyard.multiplayer;

import uk.ac.bris.cs.scotlandyard.multiplayer.model.GameStart;
import uk.ac.bris.cs.scotlandyard.multiplayer.model.Lobby;
import uk.ac.bris.cs.scotlandyard.multiplayer.model.MoveRequest;

public interface ScotlandYardClientObserver {

	//All the calls will be made from the JavaFX main thread to similar to the Spectators in the ScotlandYardGame

	//When the game lobby changes - e.g. new player, changed colour, changed ready state
	default void onLobbyChange(ScotlandYardClient client, Lobby lobby) {}

	//When the game starts
	default void onGameStarted(ScotlandYardClient client, GameStart gameStart) {}

	//When a player is requested to make a move
	default void onMoveRequested(ScotlandYardClient client, MoveRequest request) {}

	//When something is wrong with the connection
	default void onClientError(ScotlandYardClient client, RuntimeException e) {}

}
