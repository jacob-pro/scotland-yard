package uk.ac.bris.cs.scotlandyard.ui.model;

import uk.ac.bris.cs.scotlandyard.multiplayer.Client;
import uk.ac.bris.cs.scotlandyard.multiplayer.ScotlandYardServer;

public class MLGModel {

	public Client client;
	public ScotlandYardServer server;

	public void cleanUp() {
		//Close client first so we don't have to worry about connection exceptions bubbling up
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
