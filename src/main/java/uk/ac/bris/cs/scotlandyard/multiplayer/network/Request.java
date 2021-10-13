package uk.ac.bris.cs.scotlandyard.multiplayer.network;

public class Request extends Message {

    int streamID;
    public String action;
    public String data;

    Request() {
        this.messageType = MessageType.REQUEST;
    }

    @Override
    public void accept(MessageVisitor visitor) {
        visitor.accept(this);
    }

}
