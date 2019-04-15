package uk.ac.bris.cs.scotlandyard.multiplayer.network;

interface MessageVisitor {

	default void accept(Handshake message) {}

	default void accept(Request message) {}

	default void accept(Response message) {}

	default void accept(Notification message) {}

}
