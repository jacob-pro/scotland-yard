package uk.ac.bris.cs.scotlandyard.multiplayer.network;

public class Notification extends Message {

    public String content;
    public String name;

    public Notification(String name) {
        this.name = name;
        this.messageType = MessageType.NOTIFICATION;
    }

    @Override
    public void accept(MessageVisitor visitor) {
        visitor.accept(this);
    }
}
