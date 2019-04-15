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
import uk.ac.bris.cs.scotlandyard.multiplayer.ScotlandYardClient;
import uk.ac.bris.cs.scotlandyard.ui.Utils;
import uk.ac.bris.cs.scotlandyard.ui.model.MLGModel;

import java.net.URISyntaxException;

@BindFXML(value = "layout/MLGJoinGame.fxml", css = "style/mlg.css")
public final class MLGJoinGame implements Controller {

	@FXML private StackPane root;
	@FXML private TextField nameField;
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

		String address = this.addressField.getText();
		String username = this.nameField.getText();

		if (username.isEmpty()) {
			Utils.handleNonFatalException(new RuntimeException("Username is required"), "Username is required");
			return;
		}
		if (address.isEmpty()) {
			Utils.handleNonFatalException(new RuntimeException("Address is required"), "Address is required");
			return;
		}

		int port;
		String host;
		if (address.contains(":")) {
			host = address.substring(0, address.lastIndexOf(":"));
			try {
				port = Integer.parseInt(address.substring(address.lastIndexOf(":")+1, address.length()));
			} catch (NumberFormatException e) {
				Utils.handleNonFatalException(e, "Invalid address");
				return;
			}
		} else {
			host = address;
			port = MLGHostGame.defaultPort;
		}

		MLGModel config = new MLGModel();
		try {
			config.client = new ScotlandYardClient(host, port, this.nameField.getText());
		} catch (URISyntaxException e) {
			Utils.handleNonFatalException(e, "Invalid address");
			config.cleanUp();
			return;
		}

		//Show progress spinner
		this.progress.setVisible(true);
		config.client.connect().whenComplete((result, error) -> {
			Platform.runLater(() -> {
				//Hide spinner
				this.progress.setVisible(false);
				if (result != null) {
					MLGLobby lobby = new MLGLobby(this.startScreen, config);
					this.startScreen.pushController(lobby);
				} else {
					Utils.handleNonFatalException(error, "Connection error");
					config.cleanUp();
				}
			});
		});
	}

	private void cancelButtonAction(ActionEvent event) {
		this.startScreen.popController(this);
	}

}
