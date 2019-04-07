package uk.ac.bris.cs.scotlandyard.server;

import uk.ac.bris.cs.scotlandyard.model.Spectator;

public interface MLGObserver extends Spectator {

	default void onLobbyReceived() {}

	default void onPlayerJoined() {}

	default void onPlayerLeft() {}

	default void onGameStarted() {}

	default void onError(Exception e) {}

}
