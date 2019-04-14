package uk.ac.bris.cs.scotlandyard.network.model;

//Data that the server will send in the connection handshake
public class Join {

	public String serverName;
	public Integer maxPlayers;
	public Integer turnTimer;
	public Integer playerID;

}
