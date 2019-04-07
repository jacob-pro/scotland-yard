package uk.ac.bris.cs.scotlandyard.ui.model;

import uk.ac.bris.cs.scotlandyard.server.MLGConnection;
import uk.ac.bris.cs.scotlandyard.server.MLGServer;

public class MLGProperty {

	private MLGConnection client;
	private MLGServer server;

	public MLGProperty(MLGConnection client, MLGServer server) {
		this.client = client;
		this.server = server;
	}

	public MLGConnection client(){
		return this.client;
	}

	public MLGServer server(){
		return this.server;
	}

}
