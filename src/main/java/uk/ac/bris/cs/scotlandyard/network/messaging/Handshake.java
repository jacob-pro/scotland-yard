package uk.ac.bris.cs.scotlandyard.network.messaging;

public class Handshake extends Message {

	public String error;
	public String data;

	public Handshake() {
		this.messageType = MessageType.HANDSHAKE;
	}

	@Override
	public void accept(MessageVisitor visitor) {
		visitor.accept(this);
	}

}
