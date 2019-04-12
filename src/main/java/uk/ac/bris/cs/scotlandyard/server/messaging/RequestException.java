package uk.ac.bris.cs.scotlandyard.server.messaging;

//If a request failed

@SuppressWarnings("serial")
public class RequestException extends RuntimeException {
	public RequestException(String s) {
		super(s);
	}
}
