package uk.ac.bris.cs.scotlandyard.multiplayer.network;

public class Response extends Message {

	public int streamID;
	public String data;
	public String error;

	Response() {
		this.messageType = MessageType.RESPONSE;
	}

	@Override
	public void accept(MessageVisitor visitor) {
		visitor.accept(this);
	}
}
