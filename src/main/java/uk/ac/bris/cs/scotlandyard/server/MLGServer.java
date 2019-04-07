package uk.ac.bris.cs.scotlandyard.server;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYardGame;
import java.net.InetSocketAddress;

public class MLGServer {

	private MLGWebSocketServer webSocketServer;
	private ScotlandYardGame model = null;
	private int maxPlayers;
	private int turnTimer;
	private String serverName;
	private boolean didStart = false;
	private Exception startException;

	//Creates a server and waits for it to start
	public MLGServer(InetSocketAddress address, int maxPlayers, int turnTimer, String serverName) {
		this.maxPlayers = maxPlayers;
		this.turnTimer = turnTimer;
		this.serverName = serverName;
		this.webSocketServer = new MLGWebSocketServer(address);
		this.webSocketServer.start();
		synchronized (this) {
			try {
				this.wait();
			} catch (InterruptedException e) {
				throw new RuntimeException("Why did you interrupt me?");
			}
			if (!this.didStart) {
				throw new RuntimeException(this.startException);
			}
		}
	}

	private void startGame() {

	}

	class MLGWebSocketServer extends WebSocketServer {

		MLGWebSocketServer(InetSocketAddress address) {
			super(address);
		}

		@Override
		public void onStart() {
			synchronized (MLGServer.this) {
				MLGServer.this.didStart = true;
				MLGServer.this.notify();
			}
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
			synchronized (MLGServer.this) {
				if(!MLGServer.this.didStart) {
					MLGServer.this.startException = ex;
					MLGServer.this.notify();
				}
			}
			System.err.println("an error occured on connection " + conn.getRemoteSocketAddress()  + ":" + ex);
		}
	}


}
