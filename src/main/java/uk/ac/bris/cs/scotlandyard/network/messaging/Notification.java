package uk.ac.bris.cs.scotlandyard.network.messaging;

public class Notification extends Message {

	public String content;
	public String name;

	public Notification(Object name) {
		this.name = name.toString();
		this.messageType = MessageType.NOTIFICATION;
	}

	@Override
	public void accept(MessageVisitor visitor) {
		visitor.accept(this);
	}
}
