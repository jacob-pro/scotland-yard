package uk.ac.bris.cs.scotlandyard.ui.controller;

import javafx.application.Platform;
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
		this.launchStartScreen();
	}

	private void launchStartScreen() {
		MLGStartScreen startScreen = new MLGStartScreen(this, resourceManager);
		this.showOverlay(startScreen.root());
	}

	void startGame(MLGModel model, Join joinMessage, GameStart gameStart) {
		this.hideOverlay();
		try {
			new MLGGame.Game(model, joinMessage, gameStart);
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

		private Set<MLGBoardPlayers.MLGBoardPlayer> boardPlayers = new HashSet<>();
		private MLGModel model;
		private static final String NOTIFY_GAMEOVER = "notify_gameover";
		private final List<GameControl> controls;

		Game(MLGModel model, Join joinMessage, GameStart gameStart) {
			this.model = model;

			this.model.client.registerObserver(this);
			ModelProperty setup = gameStart.generateModelProperty(manager());
			this.controls = Arrays.asList(board, travelLog, ticketsCounter, status, this);

			gameStart.players.forEach(p -> {
				MLGBoardPlayers.MLGBoardPlayer player;
				if (p.playerID.equals(joinMessage.playerID)) {
					player = new MLGBoardPlayers.ThisPlayer(p.colour);
				} else {
					player = new MLGBoardPlayers.RemotePlayer(p.colour, p.username);
				}
				board.setBoardPlayer(p.colour, player);
				this.boardPlayers.add(player);
			});

			controls.forEach(this.model.client::registerSpectator);
			controls.forEach(l -> l.onGameAttach(this.model.client, setup));
		}

		@Override
		public void onMoveRequested(ScotlandYardClient client, MoveRequest request) {
			MLGBoardPlayers.MLGBoardPlayer player = this.boardPlayers.stream().filter(p -> p.colour == request.colour).findFirst().orElseThrow();
			//Only ThisPlayer is actually supported by the Board
			if (player instanceof MLGBoardPlayers.ThisPlayer) {
				board.makeMove(client, request.currentLocation, request.getMoves(), m -> this.model.client.makeMove(m));
			}
			Platform.runLater(() -> {		//We need this to happen after board calls their makeMove
				player.makeMoveReplacementHack(notifications, request.deadline);
			});
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
						launchStartScreen();
					}).create();
			notifications.show(NOTIFY_GAMEOVER, gameOver);
		}

		void terminate() {
			controls.forEach(model.client::unregisterSpectator);
			controls.forEach(GameControl::onGameDetached);
			this.model.cleanUp();
		}

		@Override
		public void onClientError(ScotlandYardClient client, RuntimeException e) {
			MLGGame.handleFatalException(e, this.model);
		}

	}

}
