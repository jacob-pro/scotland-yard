package uk.ac.bris.cs.scotlandyard.server.messaging;

public class Join extends Message {

	public enum Error {
		SERVER_FULL, GAME_STARTED, VERSION_MISMATCH
	}

	public Error error;
	public String serverName;
	public Integer maxPlayers;
	public Integer turnTimer;
	public Integer playerID;

	public Join() {
		this.messageType = MessageType.JOIN;
	}

	@Override
	public void accept(MessageVisitor visitor) {
		visitor.accept(this);
	}

}
