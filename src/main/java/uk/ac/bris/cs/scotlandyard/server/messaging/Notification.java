package uk.ac.bris.cs.scotlandyard.server.messaging;

public class Notification extends Message {

	public enum NotificationName {
		LOBBY_UPDATE, GAME_START, MOVE_REQUEST, MOVE_MADE, ROUND_STARTED, ROTATION_COMPLETE, GAME_OVER
	}

	public String content;
	public NotificationName name;

	public Notification(NotificationName name) {
		this.name = name;
		this.messageType = MessageType.NOTIFICATION;
	}

	@Override
	public void accept(MessageVisitor visitor) {
		visitor.accept(this);
	}
}
