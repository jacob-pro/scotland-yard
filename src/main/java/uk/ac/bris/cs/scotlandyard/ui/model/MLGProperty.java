package uk.ac.bris.cs.scotlandyard.ui.model;

import uk.ac.bris.cs.scotlandyard.server.MLGClient;
import uk.ac.bris.cs.scotlandyard.server.MLGServer;

public class MLGProperty {

	private MLGClient client;
	private MLGServer server;

	public MLGProperty(MLGClient client, MLGServer server) {
		this.client = client;
		this.server = server;
	}

	public MLGClient client(){
		return this.client;
	}

	public MLGServer server(){
		return this.server;
	}

}
