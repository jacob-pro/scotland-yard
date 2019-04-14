package uk.ac.bris.cs.scotlandyard.ui.controller;

import com.google.gson.Gson;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.StackPane;
import uk.ac.bris.cs.fxkit.BindFXML;
import uk.ac.bris.cs.fxkit.Controller;
import uk.ac.bris.cs.scotlandyard.model.Colour;
import uk.ac.bris.cs.scotlandyard.server.Observer;
import uk.ac.bris.cs.scotlandyard.server.messaging.Join;
import uk.ac.bris.cs.scotlandyard.server.messaging.Lobby;
import uk.ac.bris.cs.scotlandyard.ui.model.MLGModel;

@BindFXML(value = "layout/MLGLobby.fxml", css = "style/mlg.css")
public final class MLGLobby implements Controller, Observer {

	@FXML private StackPane root;
	@FXML private Label serverNameLabel;
	@FXML private Label maxPlayersLabel;
	@FXML private Label turnTimerLabel;
	@FXML private ToggleButton readyButton;
	@FXML private Button exitButton;

	@Override
	public Parent root() {
		return this.root;
	}

	private MLGStartScreen startScreen;
	private MLGModel config;

	MLGLobby(MLGStartScreen startScreen, MLGModel config) {
		this.startScreen = startScreen;
		this.config = config;
		Controller.bind(this);
		config.client.registerObserver(this);	//We need to sign up for lobby change and game start notifications

		Join joinMessage = config.client.joinMessage();
		this.serverNameLabel.setText("Server name: " + joinMessage.serverName);
		this.maxPlayersLabel.setText("Max players: " + joinMessage.maxPlayers.toString());
		this.turnTimerLabel.setText("Turn timer: " + (joinMessage.turnTimer == null ? "Disabled" : joinMessage.turnTimer.toString() + " seconds"));

		this.readyButton.setOnAction(this::readyButtonAction);
		this.exitButton.setOnAction(this::exitButtonAction);

		config.client.getLobby().whenComplete((result, error) -> {
			Platform.runLater(() -> {
				if (error == null) {
					this.onLobbyChange(result);
				}
			});
		});

//		config.client.setColour(Colour.BLUE).whenComplete((result1, error1) -> {
//			Platform.runLater(() -> {
//				if (error1 == null) {
//
//				}
//			});
//		});
	}

	@Override
	public void onLobbyChange(Lobby lobby) {
		Gson gson = new Gson();
		System.out.print("\n" +gson.toJson(lobby));
	}

	@Override
	public void onGameStarted() {
		this.startScreen.callBack.accept(this.config);
	}

	private void exitButtonAction(ActionEvent event) {
		this.config.cleanUp();
		this.startScreen.popController(this);
	}

	private void readyButtonAction(ActionEvent event) {

	}

}
