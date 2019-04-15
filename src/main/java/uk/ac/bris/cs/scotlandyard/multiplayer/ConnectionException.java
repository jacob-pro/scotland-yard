package uk.ac.bris.cs.scotlandyard.multiplayer;

@SuppressWarnings("serial")
public class ConnectionException extends RuntimeException {
	ConnectionException(Exception e) {
		super(e);
	}
	ConnectionException(String s) {
		super(s);
	}
}