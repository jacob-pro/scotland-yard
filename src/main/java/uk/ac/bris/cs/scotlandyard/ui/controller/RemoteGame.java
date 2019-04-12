package uk.ac.bris.cs.scotlandyard.ui.controller;

import javafx.scene.Scene;
import javafx.stage.Stage;
import uk.ac.bris.cs.scotlandyard.ResourceManager;
import uk.ac.bris.cs.scotlandyard.model.Spectator;
import uk.ac.bris.cs.scotlandyard.ui.model.BoardProperty;
import uk.ac.bris.cs.scotlandyard.ui.model.MLGProperty;

import java.net.URL;
import java.util.ResourceBundle;

import static uk.ac.bris.cs.scotlandyard.ui.Utils.handleFatalException;

public class RemoteGame extends BaseGame implements Spectator {

	public RemoteGame(ResourceManager manager, Stage stage) {
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

		MLGStartScreen startScreen = new MLGStartScreen(resourceManager, this::startGame);
		showOverlay(startScreen.root());
	}

	private void startGame(MLGProperty config) {
		hideOverlay();
		try {
			throw new RuntimeException();
			//RemoteGame.Game game = new RemoteGame.Game(setup);
		} catch (Exception e) {
			e.printStackTrace();
			handleFatalException(e);
		}

	}
}
