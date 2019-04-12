package uk.ac.bris.cs.scotlandyard.server;

class Counter {

	private int counter = 0;

	int next() {
		this.counter++;
		return this.counter;
	}
}
