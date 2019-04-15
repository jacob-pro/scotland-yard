package uk.ac.bris.cs.scotlandyard.ui.controller;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;
import uk.ac.bris.cs.scotlandyard.ResourceManager;
import uk.ac.bris.cs.scotlandyard.ai.AIPool;
import uk.ac.bris.cs.scotlandyard.harness.CodeGenRecorder;
import uk.ac.bris.cs.scotlandyard.harness.GameModelSequencePUMLCodeGen;
import uk.ac.bris.cs.scotlandyard.harness.TestHarnessPlayOutTestCodeGen;
import uk.ac.bris.cs.scotlandyard.model.*;
import uk.ac.bris.cs.scotlandyard.multiplayer.ModelPropertyGenerator;
import uk.ac.bris.cs.scotlandyard.multiplayer.Observer;
import uk.ac.bris.cs.scotlandyard.multiplayer.ScotlandYardClient;
import uk.ac.bris.cs.scotlandyard.multiplayer.ScotlandYardServer;
import uk.ac.bris.cs.scotlandyard.ui.GameControl;
import uk.ac.bris.cs.scotlandyard.ui.Utils;
import uk.ac.bris.cs.scotlandyard.ui.model.*;

import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

import static io.atlassian.fugue.Option.fromOptional;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;

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

	void startGame(MLGModel config) {
		hideOverlay();
		try {
			MLGGame.Game game = new MLGGame.Game(config);
		} catch (Exception e) {
			e.printStackTrace();
			MLGGame.handleFatalException(e, config);
		}
	}

	static void handleFatalException(Throwable throwable, MLGModel model) {
		//Make sure the server always gets shutdown
		model.cleanUp();
		Utils.handleFatalException(throwable);
	}

	private class Game implements Observer, GameControl {

		private MLGModel config;
		private static final String NOTIFY_GAMEOVER = "notify_gameover";
		private final ScotlandYardClient model;
		private final List<GameControl> controls;
		private final AIPool<Side> pool = new AIPool<>(createVisualiserSurface(), e -> MLGGame.handleFatalException(e, this.config));

		Game(MLGModel config) {
			this.config = config;
			this.model = config.client;

			ModelProperty setup = ModelPropertyGenerator.modelPropertyForColours(manager(), new HashSet<>(this.model.getPlayers()));
			this.controls = asList(//board,
					travelLog, ticketsCounter, status, this);

			pool.initialise(resourceManager, this.model);
			// Add all players to board
			for (PlayerProperty property : setup.players()) {
				board.setBoardPlayer(property.colour(),BoardPlayers.resolve(
								fromOptional(pool.createPlayer(property.colour())),
								fromOptional(property.name()),
								() -> onGameOver(model, model.getCurrentPlayer().isDetective()
										? ImmutableSet.of(Colour.BLACK)
										: ImmutableSet.copyOf(model.getPlayers().stream()
										.filter(Colour::isDetective)
										.collect(toList())))));
			}

			controls.forEach(model::registerSpectator);
			controls.forEach(l -> l.onGameAttach(model, setup));
		}

		void terminate() {

		}

		@Override
		public void onGameOver(ScotlandYardView view, Set<Colour> winningPlayers) {

		}

		@Override
		public void onClientError(RuntimeException e) {
			MLGGame.handleFatalException(e, this.config);
		}
	}

}
