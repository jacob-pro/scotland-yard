package uk.ac.bris.cs.scotlandyard.multiplayer.network;

public interface ClientDelegate {

	//After the client connects, message from handshake on success, exception if it failed
	void clientDidConnect(Client c, String data, ConnectionException e);

	//When a notification arrives
	void clientReceivedNotification(Client c, String name, String content);

	//When something went wrong
	void clientDidError(Client c, ConnectionException e);

	//When the server disconnects us
	void clientWasDisconnected(Client c);

}
