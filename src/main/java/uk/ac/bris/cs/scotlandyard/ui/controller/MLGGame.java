package uk.ac.bris.cs.scotlandyard.ui.controller;

import javafx.scene.Scene;
import javafx.scene.control.MenuItem;
import javafx.stage.Stage;
import uk.ac.bris.cs.scotlandyard.ResourceManager;
import uk.ac.bris.cs.scotlandyard.model.Spectator;
import uk.ac.bris.cs.scotlandyard.server.MLGClient;
import uk.ac.bris.cs.scotlandyard.ui.model.BoardProperty;
import uk.ac.bris.cs.scotlandyard.ui.model.MLGProperty;
import uk.ac.bris.cs.scotlandyard.ui.model.ModelProperty;

import java.net.URL;
import java.util.ResourceBundle;

import static uk.ac.bris.cs.scotlandyard.ui.Utils.handleFatalException;

public class MLGGame extends BaseGame implements Spectator {

	public MLGGame(ResourceManager manager, Stage stage) {
		super(manager, stage, new BoardProperty());
		stage.setTitle("MLG ScotlandYard");
		stage.setScene(new Scene(this.root()));
		stage.getIcons().add(manager.getImage(ResourceManager.ImageResource.ICON));
		stage.show();
	}

	@Override
	public void initialize(URL location, ResourceBundle resources) {

		//We need to add some buttons here
//		MenuItem newGame = new MenuItem("New game");
//		newGame.setOnAction(e -> LocalGame.newGame(resourceManager, new Stage(), false));
//		addMenuItem(newGame);
//
//		MenuItem showTests = new MenuItem("Restart in test capture mode");
//		showTests.setOnAction(e -> {
//			getStage().close();
//			LocalGame.newGame(resourceManager, new Stage(), true);
//		});
//		addMenuItem(showTests);

		MLGStartScreen startScreen = new MLGStartScreen(resourceManager, this.config, this::startGame);
		showOverlay(startScreen.root());
	}

	private void startGame(MLGProperty config) {
		hideOverlay();
		try {
			throw new RuntimeException();
			//MLGGame.Game game = new MLGGame.Game(setup);
		} catch (Exception e) {
			e.printStackTrace();
			handleFatalException(e);
		}

	}
}
