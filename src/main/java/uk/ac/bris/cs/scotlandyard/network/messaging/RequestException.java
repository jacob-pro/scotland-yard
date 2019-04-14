package uk.ac.bris.cs.scotlandyard.network.messaging;

//If a request failed

@SuppressWarnings("serial")
public class RequestException extends RuntimeException {
	public RequestException(String s) {
		super(s);
	}
}
