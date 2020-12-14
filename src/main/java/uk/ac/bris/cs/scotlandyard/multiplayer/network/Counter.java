package uk.ac.bris.cs.scotlandyard.multiplayer.network;

public class Counter {

    private int counter = 0;

    public int next() {
        this.counter++;
        return this.counter;
    }
}
