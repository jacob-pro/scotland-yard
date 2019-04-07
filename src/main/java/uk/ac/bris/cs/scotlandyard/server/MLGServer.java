package uk.ac.bris.cs.scotlandyard.server;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYardGame;
import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

public class MLGServer extends WebSocketServer {

	//Creates a server and waits for it to start
	public static Future<MLGServer> CreateMLGServer(InetSocketAddress address, int maxPlayers, int turnTimer, String serverName) {
		MLGServer server = new MLGServer(address, maxPlayers, turnTimer, serverName);
		server.start();
		return server.creationFuture;
	}

	private CompletableFuture<MLGServer> creationFuture = new CompletableFuture<>();
	private ScotlandYardGame model = null;
	private int maxPlayers;
	private int turnTimer;
	private String serverName;

	private MLGServer(InetSocketAddress address, int maxPlayers, int turnTimer, String serverName) {
		super(address);
		this.maxPlayers = maxPlayers;
		this.turnTimer = turnTimer;
		this.serverName = serverName;
	}

	private void startGame() {

	}

	@Override
	public void onStart() {
		this.creationFuture.complete(this);
	}

	@Override
	public void onOpen(WebSocket conn, ClientHandshake handshake) {
		if (MLGServer.this.model != null) {
			conn.send("Game already started"); //This method sends a message to the new client
			conn.close();
		}
		conn.send("Welcome to "+ this.serverName);
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
		if(!this.creationFuture.isDone()) {
			this.creationFuture.completeExceptionally(ex);
		}
		System.err.println("an error occured on connection " + conn.getRemoteSocketAddress()  + ":" + ex);
	}


}
