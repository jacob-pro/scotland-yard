package uk.ac.bris.cs.scotlandyard.ui.controller;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import uk.ac.bris.cs.fxkit.BindFXML;
import uk.ac.bris.cs.fxkit.Controller;
import uk.ac.bris.cs.scotlandyard.server.MLGConnection;
import uk.ac.bris.cs.scotlandyard.ui.model.MLGProperty;

import java.net.URISyntaxException;
import java.util.concurrent.ExecutionException;


@BindFXML(value = "layout/MLGJoinGame.fxml", css = "style/mlg.css")
public final class MLGJoinGame implements Controller {

	@FXML private StackPane root;
	@FXML private TextField addressField;
	@FXML private Button joinButton;
	@FXML private Button cancelButton;
	@FXML private VBox progress;

	@Override
	public Parent root() {
		return this.root;
	}

	private MLGStartScreen startScreen;

	MLGJoinGame(MLGStartScreen startScreen) {
		this.startScreen = startScreen;
		Controller.bind(this);
		this.joinButton.setOnAction(this::joinButtonAction);
		this.cancelButton.setOnAction(this::cancelButtonAction);
	}

	private void joinButtonAction(ActionEvent event) {

		String text = this.addressField.getText();
		int port;
		String host;
		if (text.contains(":")) {
			host = text.substring(0, text.lastIndexOf(":"));
			port = Integer.parseInt(text.substring(text.lastIndexOf(":")+1, text.length()));
		} else {
			host = text;
			port = MLGHostGame.defaultPort;
		}

		this.progress.setVisible(true);
		MLGConnection.CreateMLGConnection(host, port).whenComplete((result, exception) -> {
			Platform.runLater(() -> {
				this.progress.setVisible(false);
				if (result != null) {
					MLGProperty config = new MLGProperty(result, null);
					MLGLobby lobby = new MLGLobby(this.startScreen, config);
					this.startScreen.pushController(lobby);
				} else {
					exception.printStackTrace();
				}
			});
		});
	}

	private void cancelButtonAction(ActionEvent event) {
		this.startScreen.popController(this);
	}



}
