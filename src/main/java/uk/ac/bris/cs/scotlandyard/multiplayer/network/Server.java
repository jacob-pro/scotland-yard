package uk.ac.bris.cs.scotlandyard.multiplayer.network;

import com.google.gson.Gson;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import java.net.InetSocketAddress;

public class Server extends WebSocketServer {

	@SuppressWarnings("WeakerAccess")
	public ServerDelegate delegate;
	private MessageDeserializer messageDeserializer = new MessageDeserializer();
	private Gson gson = new Gson();
	private boolean started = false;

	public Server(InetSocketAddress address, ServerDelegate delegate) {
		super(address);
		this.delegate = delegate;
	}

	@Override
	public void onStart() {
		this.started = true;
		this.delegate.serverDidStart(this,null);
	}

	@Override
	public void onOpen(WebSocket conn, ClientHandshake handshake) {
		Handshake response = new Handshake();
		try {
			response.data = this.delegate.serverReceivedConnection(this, handshake, conn);
			conn.send(gson.toJson(response));
		} catch (Exception e) {
			response.error = e.getMessage();
			conn.send(gson.toJson(response));
			conn.close();
		}
	}

	@Override
	public void onClose(WebSocket conn, int code, String reason, boolean remote) {
		if (remote) {
			this.delegate.serverClientDisconnected(this, conn);
		}
	}

	@Override
	public void onMessage(WebSocket conn, String string) {
		this.messageDeserializer.deserialize(string).ifPresent(m -> m.accept(new MessageVisitor() {
			//Incoming requests
			@Override
			public void accept(Request message) {
				Response response = new Response();
				response.streamID = message.streamID;
				delegate.serverReceivedRequest(Server.this, message, response, conn);
				conn.send(gson.toJson(response));
			}
		}));
	}

	@Override
	public void onError(WebSocket conn, Exception ex) {
		// Note that if error is fatal onClose is also called
		if(!this.started) {
			this.delegate.serverDidStart(this, ex);
		}
	}

}
