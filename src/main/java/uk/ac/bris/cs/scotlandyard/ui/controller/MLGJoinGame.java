package uk.ac.bris.cs.scotlandyard.ui.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import uk.ac.bris.cs.fxkit.BindFXML;
import uk.ac.bris.cs.fxkit.Controller;
import uk.ac.bris.cs.scotlandyard.server.MLGClient;
import uk.ac.bris.cs.scotlandyard.ui.model.MLGProperty;

import java.net.InetSocketAddress;


@BindFXML(value = "layout/MLGJoinGame.fxml", css = "style/mlg.css")
public final class MLGJoinGame implements Controller {

	@FXML private StackPane root;
	@FXML private TextField addressField;
	@FXML private Button joinButton;
	@FXML private Button cancelButton;

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

	private InetSocketAddress stringToSocket(String text) {
		int port;
		String host;
		if (text.contains(":")) {
			host = text.substring(0, text.lastIndexOf(":"));
			port = Integer.parseInt(text.substring(text.lastIndexOf(":")+1, text.length()));
		} else {
			host = text;
			port = MLGHostGame.defaultPort;
		}
		return new InetSocketAddress(host, port);
	}

	private void joinButtonAction(ActionEvent event) {

		InetSocketAddress socket = this.stringToSocket(this.addressField.getText());
		MLGClient client = new MLGClient(socket);
		client.connect();

		MLGProperty config = new MLGProperty(client, null);
		MLGLobby lobby = new MLGLobby(this.startScreen, config);
		this.startScreen.pushController(lobby);
	}

	private void cancelButtonAction(ActionEvent event) {
		this.startScreen.popController(this);
	}



}
