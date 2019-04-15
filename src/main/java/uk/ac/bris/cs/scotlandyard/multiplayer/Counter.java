package uk.ac.bris.cs.scotlandyard.multiplayer;

class Counter {

	private int counter = 0;

	int next() {
		this.counter++;
		return this.counter;
	}
}
