package uk.ac.bris.cs.scotlandyard.multiplayer;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import uk.ac.bris.cs.scotlandyard.ResourceManager;
import uk.ac.bris.cs.scotlandyard.model.Colour;
import uk.ac.bris.cs.scotlandyard.model.StandardGame;
import uk.ac.bris.cs.scotlandyard.ui.model.ModelProperty;
import uk.ac.bris.cs.scotlandyard.ui.model.PlayerProperty;

import java.util.*;

import static java.util.stream.Collectors.toList;

public class ModelPropertyGenerator {

	public static ModelProperty modelPropertyForColours(ResourceManager manager, Set<Colour> enabledColours) {

		ModelProperty defaults = ModelProperty.createDefault(manager);
		ObservableList<PlayerProperty> enabledPlayers = FXCollections.observableArrayList(defaults.allPlayers().stream()
				.filter(p -> enabledColours.contains(p.colour())).collect(toList()));

		return new ModelProperty(null, defaults.revealRounds(), enabledPlayers, defaults.graphProperty().get());
	}

	@SuppressWarnings("Duplicates")		//Copied from GameSetup
	static ModelProperty randomisedLocations(ModelProperty property) {

		ObservableList<PlayerProperty> players = property.players();

		ArrayList<Integer> availableLocation = new ArrayList<>(StandardGame.DETECTIVE_LOCATIONS);
		Collections.shuffle(availableLocation);
		ArrayDeque<Integer> deque = new ArrayDeque<>(availableLocation);
		players.forEach(p -> p.locationProperty().unbind());
		players.filtered(PlayerProperty::randomLocation).forEach(p -> {
			if (p.mrX()) {
				p.locationProperty().set(StandardGame.MRX_LOCATIONS
						.get(new Random().nextInt(StandardGame.MRX_LOCATIONS.size())));
			} else {
				p.locationProperty().set(deque.pop());
			}
		});

		return new ModelProperty(null, property.revealRounds(), players, property.graphProperty().get());
	}
}
