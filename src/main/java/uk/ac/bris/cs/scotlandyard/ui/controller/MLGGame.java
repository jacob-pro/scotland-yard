package uk.ac.bris.cs.scotlandyard.ui.controller;

import javafx.scene.Scene;
import javafx.stage.Stage;
import uk.ac.bris.cs.scotlandyard.ResourceManager;
import uk.ac.bris.cs.scotlandyard.model.Colour;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYardView;
import uk.ac.bris.cs.scotlandyard.model.Spectator;
import uk.ac.bris.cs.scotlandyard.multiplayer.ScotlandYardClientObserver;
import uk.ac.bris.cs.scotlandyard.multiplayer.ScotlandYardClient;
import uk.ac.bris.cs.scotlandyard.multiplayer.model.GameStart;
import uk.ac.bris.cs.scotlandyard.ui.GameControl;
import uk.ac.bris.cs.scotlandyard.ui.Utils;
import uk.ac.bris.cs.scotlandyard.ui.model.BoardProperty;
import uk.ac.bris.cs.scotlandyard.ui.model.MLGModel;
import uk.ac.bris.cs.scotlandyard.ui.model.ModelProperty;

import java.net.URL;
import java.util.*;


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
		MLGStartScreen startScreen = new MLGStartScreen(this, resourceManager);
		showOverlay(startScreen.root());
	}

	void startGame(MLGModel model, GameStart gameStart) {
		hideOverlay();
		try {
			MLGGame.Game game = new MLGGame.Game(model, gameStart);
		} catch (Exception e) {
			MLGGame.handleFatalException(e, model);
		}
	}

	static void handleFatalException(Throwable throwable, MLGModel model) {
		//Make sure the server always gets shutdown
		model.cleanUp();
		Utils.handleFatalException(throwable);
	}

	private class Game implements ScotlandYardClientObserver, GameControl {

		private MLGModel model;
		private static final String NOTIFY_GAMEOVER = "notify_gameover";
		private final List<GameControl> controls;

		Game(MLGModel model, GameStart gameStart) {
			this.model = model;

			ModelProperty setup = gameStart.generateModelProperty(manager());
			this.controls = Arrays.asList(travelLog, ticketsCounter, status, this);

			controls.forEach(this.model.client::registerSpectator);
			controls.forEach(l -> l.onGameAttach(this.model.client, setup));
		}

		void terminate() {

		}

		@Override
		public void onGameOver(ScotlandYardView view, Set<Colour> winningPlayers) {

		}

		@Override
		public void onClientError(ScotlandYardClient client, RuntimeException e) {
			MLGGame.handleFatalException(e, this.model);
		}
	}

}
