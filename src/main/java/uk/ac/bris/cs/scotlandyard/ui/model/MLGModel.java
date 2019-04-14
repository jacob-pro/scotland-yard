package uk.ac.bris.cs.scotlandyard.ui.model;

import uk.ac.bris.cs.scotlandyard.server.Client;
import uk.ac.bris.cs.scotlandyard.server.Server;

public class MLGModel {

	public Client client;
	public Server server;

	public void cleanUp() {
		if (this.client != null) {
			this.client.close();
			this.client = null;
		}
		if (this.server != null) {
			this.server.close();
			this.server = null;
		}
	}

}
