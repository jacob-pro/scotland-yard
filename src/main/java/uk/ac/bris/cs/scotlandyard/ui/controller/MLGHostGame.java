package uk.ac.bris.cs.scotlandyard.ui.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import uk.ac.bris.cs.fxkit.BindFXML;
import uk.ac.bris.cs.fxkit.Controller;
import uk.ac.bris.cs.scotlandyard.model.Colour;
import uk.ac.bris.cs.scotlandyard.server.MLGConnection;
import uk.ac.bris.cs.scotlandyard.server.MLGServer;
import uk.ac.bris.cs.scotlandyard.ui.model.MLGProperty;

import java.net.InetSocketAddress;
import java.net.URISyntaxException;
import java.util.concurrent.ExecutionException;

@BindFXML(value = "layout/MLGHostGame.fxml", css = "style/mlg.css")
public final class MLGHostGame implements Controller {

	static Integer defaultPort = 25566;

	@FXML private StackPane root;
	@FXML private ChoiceBox<Colour> colourChoiceBox;
	@FXML private TextField serverName;
	@FXML private CheckBox turnTimerCheckBox;
	@FXML private TextField turnTimerField;
	@FXML private Slider maxPlayersSlider;
	@FXML private TextField portField;
	@FXML private Button hostButton;
	@FXML private Button cancelButton;

	@Override
	public Parent root() {
		return this.root;
	}

	private MLGStartScreen startScreen;

	MLGHostGame(MLGStartScreen startScreen) {
		this.startScreen = startScreen;
		Controller.bind(this);
		this.portField.setText(MLGHostGame.defaultPort.toString());
		this.turnTimerCheckBox.setOnAction(a -> {
			this.turnTimerField.setDisable(!this.turnTimerCheckBox.isSelected());
			this.turnTimerField.setText(null);
		});
		this.hostButton.setOnAction(this::hostButtonAction);
		this.cancelButton.setOnAction(this::cancelButtonAction);
		this.colourChoiceBox.getItems().setAll(Colour.values());

	}

	private void hostButtonAction(ActionEvent event) {
		int port = Integer.parseInt(this.portField.getText());
		int maxPlayers = (int) this.maxPlayersSlider.getValue();
		int turnTimer = this.turnTimerCheckBox.isSelected() ? Integer.parseInt(this.turnTimerField.getText()): 0;

		String localhost = "localhost";
		InetSocketAddress address = new InetSocketAddress(localhost, port);
		MLGServer server = new MLGServer(address, maxPlayers, turnTimer, this.serverName.getText());

		try {
			MLGConnection connection = MLGConnection.CreateMLGConnection(localhost, port).get();
			MLGProperty config = new MLGProperty(connection, server);
			MLGLobby lobby = new MLGLobby(this.startScreen, config);
			this.startScreen.pushController(lobby);
		} catch (InterruptedException | ExecutionException e) {
			throw new RuntimeException(e);
		}
	}

	private void cancelButtonAction(ActionEvent event) {
		this.startScreen.popController(this);
	}

}
