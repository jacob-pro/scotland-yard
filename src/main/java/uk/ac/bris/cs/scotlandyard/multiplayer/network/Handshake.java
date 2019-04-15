package uk.ac.bris.cs.scotlandyard.multiplayer.network;

public class Handshake extends Message {

	public String error;
	public String data;

	Handshake() {
		this.messageType = MessageType.HANDSHAKE;
	}

	@Override
	public void accept(MessageVisitor visitor) {
		visitor.accept(this);
	}

}
