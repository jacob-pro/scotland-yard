package uk.ac.bris.cs.scotlandyard.multiplayer.model;

import uk.ac.bris.cs.scotlandyard.ResourceManager;
import uk.ac.bris.cs.scotlandyard.ui.model.ModelProperty;

import java.util.List;
import java.util.Optional;

public class GameStart {

    public List<GameStartPlayer> players;
    public List<Boolean> rounds;

    //Create a model property that is the same as the server would have made
    public ModelProperty generateModelProperty(ResourceManager manager) {
        ModelProperty defaults = ModelProperty.createDefault(manager);
        defaults.timeoutProperty().setValue(null);
        defaults.allPlayers().forEach(playerProperty -> {
            Optional<GameStartPlayer> match = this.players.stream().filter(p -> p.colour == playerProperty.colour()).findAny();
            match.ifPresentOrElse(player -> {
                playerProperty.enabledProperty().setValue(true);
                playerProperty.locationProperty().setValue(player.startLocation);
            }, () -> {
                playerProperty.enabledProperty().setValue(false);
            });
        });
        defaults.revealRounds().setAll(this.rounds);
        return defaults;
    }

}
