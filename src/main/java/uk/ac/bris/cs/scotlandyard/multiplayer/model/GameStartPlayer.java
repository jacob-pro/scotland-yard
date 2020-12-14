package uk.ac.bris.cs.scotlandyard.multiplayer.model;

import uk.ac.bris.cs.scotlandyard.model.Colour;

public class GameStartPlayer {

    public Colour colour;
    public Integer startLocation;
    public Integer playerID;
    public String username;

    public GameStartPlayer(Colour colour) {
        this.colour = colour;
    }

}
