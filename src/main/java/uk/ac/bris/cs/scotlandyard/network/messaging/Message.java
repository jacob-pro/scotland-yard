package uk.ac.bris.cs.scotlandyard.network.messaging;

//An abstract class that represents the different types of messages that form the network protocol
abstract public class Message {

	enum MessageType {
		JOIN, 			//Part of the connection handshake
		REQUEST, 		//A request that is sent, a matching response is required
		RESPONSE, 		//A response for a request.
		NOTIFICATION	//A one way notification, no response is required
	}

	MessageType messageType;

	abstract public void accept(MessageVisitor visitor);

}

