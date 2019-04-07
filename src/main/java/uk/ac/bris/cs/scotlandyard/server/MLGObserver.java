package uk.ac.bris.cs.scotlandyard.server;

import uk.ac.bris.cs.scotlandyard.model.Spectator;

public interface MLGObserver extends Spectator {

	default void onDisconnection() {}

	default void onConnection() {}

}
