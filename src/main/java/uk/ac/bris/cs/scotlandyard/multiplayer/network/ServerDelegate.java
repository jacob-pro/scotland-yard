package uk.ac.bris.cs.scotlandyard.multiplayer.network;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;

public interface ServerDelegate {

	//After the server starts, exception if it failed
	void serverDidStart(Server s, Exception e);

	//Return a string to be sent to the client, throw an exception to reject the connection
	String serverReceivedConnection(Server s, ClientHandshake clientHandshake, WebSocket conn) throws ServerJoinException;

	//Populate the response with either error or success
	void serverReceivedRequest(Server s, Request request, Response response, WebSocket conn);

	//Notification that a client has disconnected
	void serverClientDisconnected(Server s, WebSocket conn);

}
