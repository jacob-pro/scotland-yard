package uk.ac.bris.cs.scotlandyard.server.messaging;

public class Response extends Message {

	public int streamID;
	public String data;
	public String error;

	public Response() {
		this.messageType = MessageType.RESPONSE;
	}

	@Override
	public void accept(MessageVisitor visitor) {
		visitor.accept(this);
	}
}
