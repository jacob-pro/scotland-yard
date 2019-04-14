package uk.ac.bris.cs.scotlandyard.network.messaging;

public class Request extends Message {

	public enum Action {
		GET_LOBBY, SET_COLOUR, SET_READY, MAKE_MOVE
	}

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
