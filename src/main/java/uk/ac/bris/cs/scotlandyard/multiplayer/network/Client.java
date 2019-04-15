package uk.ac.bris.cs.scotlandyard.multiplayer.network;

import com.google.gson.Gson;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class Client extends WebSocketClient {

	@SuppressWarnings("WeakerAccess")
	public ClientDelegate delegate;
	private Counter streamIDCounter = new Counter();
	private Map<Integer, CompletableFuture<String>> pendingRequests = new HashMap<>();
	private MessageDeserializer messageDeserializer = new MessageDeserializer();
	private Gson gson = new Gson();
	private boolean started = false;

	//Timeout is in milliseconds - Java Socket connect()
	public Client(URI uri, Map<String, String> headers, int connectTimeout, ClientDelegate delegate) {
		super(uri, new Draft_6455(), headers, connectTimeout);
		this.delegate = delegate;
	}

	public CompletableFuture<String> performRequest(Object action, String data) {
		CompletableFuture<String> future = new CompletableFuture<>();
		Request request = new Request();
		request.streamID = this.streamIDCounter.next();
		request.data = data;
		request.action = action.toString();
		this.pendingRequests.put(request.streamID, future);
		this.send(this.gson.toJson(request));
		return future;
	}

	@Override
	public void onMessage(String string) {
		this.messageDeserializer.deserialize(string).ifPresent(m -> m.accept(new MessageVisitor() {
			@Override
			public void accept(Handshake message) {
				Client.this.started = true;
				if (message.error == null) {
					delegate.clientDidConnect(Client.this, message.data, null);
				} else {
					delegate.clientDidConnect(Client.this, null, new ConnectionException(message.error));
				}
			}
			@Override
			public void accept(Response message) {
				CompletableFuture<String> future = Client.this.pendingRequests.get(message.streamID);
				if (future != null) {
					if (message.error == null) {
						future.complete(message.data);
					} else {
						future.completeExceptionally(new RequestException(message.error));
					}
				}
			}
			@Override
			public void accept(Notification message) {
				if (message.name == null) return;
				delegate.clientReceivedNotification(Client.this, message.name, message.content);
			}
		}));
	}

	@Override
	public void onError(Exception e) {
		// If the error occurs on creation we will fail the creation promise e.g. couldn't connect to the network
		// I don't really know what sort of errors are possible later on, other than disconnection?
		if(!this.started) {
			this.delegate.clientDidConnect(this, null, new ConnectionException(e));
		} else {
			this.delegate.clientDidError(this, new ConnectionException(e));
		}
	}

	@Override
	public void onOpen(ServerHandshake handshake) {
	}

	@Override
	public void onClose(int code, String reason, boolean remote) {
		if(remote) {
			this.delegate.clientWasDisconnected(this);
		}
	}

}
