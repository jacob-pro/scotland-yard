package uk.ac.bris.cs.scotlandyard.ui.controller;

import javafx.scene.Scene;
import javafx.stage.Stage;
import uk.ac.bris.cs.scotlandyard.ResourceManager;
import uk.ac.bris.cs.scotlandyard.model.Colour;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYardView;
import uk.ac.bris.cs.scotlandyard.model.Spectator;
import uk.ac.bris.cs.scotlandyard.multiplayer.ScotlandYardClient;
import uk.ac.bris.cs.scotlandyard.multiplayer.ScotlandYardClientObserver;
import uk.ac.bris.cs.scotlandyard.multiplayer.model.GameStart;
import uk.ac.bris.cs.scotlandyard.multiplayer.model.Join;
import uk.ac.bris.cs.scotlandyard.multiplayer.model.MoveRequest;
import uk.ac.bris.cs.scotlandyard.ui.GameControl;
import uk.ac.bris.cs.scotlandyard.ui.Utils;
import uk.ac.bris.cs.scotlandyard.ui.model.BoardProperty;
import uk.ac.bris.cs.scotlandyard.ui.model.MLGModel;
import uk.ac.bris.cs.scotlandyard.ui.model.ModelProperty;

import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;


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
		this.setupGame();
	}

	private void setupGame() {
		MLGStartScreen startScreen = new MLGStartScreen(this, resourceManager);
		showOverlay(startScreen.root());
	}

	void startGame(MLGModel model, Join joinMessage, GameStart gameStart) {
		hideOverlay();
		try {
			MLGGame.Game game = new MLGGame.Game(model, joinMessage, gameStart);
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

		Game(MLGModel model, Join joinMessage, GameStart gameStart) {
			this.model = model;

			this.model.client.registerObserver(this);
			ModelProperty setup = gameStart.generateModelProperty(manager());
			this.controls = Arrays.asList(board, travelLog, ticketsCounter, status, this);

			gameStart.players.forEach(p -> {
				if (p.playerID.equals(joinMessage.playerID)) {
					board.setBoardPlayer(p.colour, new MLGBoardPlayers.ThisPlayer());
				} else {
					board.setBoardPlayer(p.colour, new MLGBoardPlayers.RemotePlayer(p.colour, p.username));
				}
			});

			controls.forEach(this.model.client::registerSpectator);
			controls.forEach(l -> l.onGameAttach(this.model.client, setup));
		}

		void terminate() {
			controls.forEach(model.client::unregisterSpectator);
			controls.forEach(GameControl::onGameDetached);
			this.model.cleanUp();
		}

		@Override
		public void onMoveRequested(ScotlandYardClient client, MoveRequest request) {
			if (request.ourMove()) {
				board.makeMove(client, request.currentLocation, request.getMoves(), m -> {
					this.model.client.makeMove(m);
				});
			}
		}

		@Override
		public void onGameOver(ScotlandYardView view, Set<Colour> winningPlayers) {
			board.lock();
			notifications.dismissAll();
			Notifications.NotificationBuilder.Notification gameOver = new Notifications.NotificationBuilder(
					"Game over, winner is " + winningPlayers)
					.addAction("Main menu", () -> {
						notifications.dismissAll();
						terminate();
						setupGame();
					}).create();
			notifications.show(NOTIFY_GAMEOVER, gameOver);
		}

		@Override
		public void onClientError(ScotlandYardClient client, RuntimeException e) {
			MLGGame.handleFatalException(e, this.model);
		}

	}

}
