package uk.ac.bris.cs.scotlandyard.server.messaging;

public interface MessageVisitor {

	default void accept(Join message) {}

	default void accept(Request message) {}

	default void accept(Response message) {}

	default void accept(Notification message) {}

}
