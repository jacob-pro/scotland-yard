package uk.ac.bris.cs.scotlandyard.ui.model;

import uk.ac.bris.cs.scotlandyard.server.Client;
import uk.ac.bris.cs.scotlandyard.server.Server;

public class MLGProperty {

	private Client client;
	private Server server;

	public MLGProperty(Client client, Server server) {
		this.client = client;
		this.server = server;
	}

	public Client client(){
		return this.client;
	}

	public Server server(){
		return this.server;
	}

}
