package uk.ac.bris.cs.scotlandyard.server;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYardGame;
import java.net.InetSocketAddress;

public class MLGServer {

	private MLGWebSocketServer webSocketServer;
	private ScotlandYardGame model = null;

	public MLGServer(InetSocketAddress address, int maxPlayers, int turnTimer, String name) {
		this.webSocketServer = new MLGWebSocketServer(address);
	}

	private void createModel() {

	}

	class MLGWebSocketServer extends WebSocketServer {

		MLGWebSocketServer(InetSocketAddress address) {
			super(address);
		}

		@Override
		public void onOpen(WebSocket conn, ClientHandshake handshake) {
			if (MLGServer.this.model != null) {
				conn.send("Game already started"); //This method sends a message to the new client
				conn.close();
			}
			broadcast( "new connection: " + handshake.getResourceDescriptor() ); //This method sends a message to all clients connected
			System.out.println("new connection to " + conn.getRemoteSocketAddress());
		}

		@Override
		public void onClose(WebSocket conn, int code, String reason, boolean remote) {
			System.out.println("closed " + conn.getRemoteSocketAddress() + " with exit code " + code + " additional info: " + reason);
		}

		@Override
		public void onMessage(WebSocket conn, String message) {
			System.out.println("received message from "	+ conn.getRemoteSocketAddress() + ": " + message);
		}

		@Override
		public void onError(WebSocket conn, Exception ex) {
			System.err.println("an error occured on connection " + conn.getRemoteSocketAddress()  + ":" + ex);
		}

		@Override
		public void onStart() {
			System.out.println("server started successfully");
		}
	}


}
