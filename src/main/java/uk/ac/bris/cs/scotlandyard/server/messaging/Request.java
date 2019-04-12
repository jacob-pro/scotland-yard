package uk.ac.bris.cs.scotlandyard.server.messaging;

public class Request extends Message {

	public int streamID;
	public Action action;
	public String data;

	public Request() {
		this.messageType = MessageType.REQUEST;
	}

	@Override
	public void accept(MessageVisitor visitor) {
		visitor.accept(this);
	}

}
