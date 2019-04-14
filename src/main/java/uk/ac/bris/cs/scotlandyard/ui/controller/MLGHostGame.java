package uk.ac.bris.cs.scotlandyard.ui.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import uk.ac.bris.cs.fxkit.BindFXML;
import uk.ac.bris.cs.fxkit.Controller;
import uk.ac.bris.cs.scotlandyard.server.Client;
import uk.ac.bris.cs.scotlandyard.server.Server;
import uk.ac.bris.cs.scotlandyard.ui.model.MLGModel;

import java.net.InetSocketAddress;
import java.net.URISyntaxException;
import java.util.concurrent.ExecutionException;

@BindFXML(value = "layout/MLGHostGame.fxml", css = "style/mlg.css")
public final class MLGHostGame implements Controller {

	static Integer defaultPort = 25566;

	@FXML private StackPane root;
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
	}

	private void hostButtonAction(ActionEvent event) {
		int port = Integer.parseInt(this.portField.getText());
		int maxPlayers = (int) this.maxPlayersSlider.getValue();
		Integer turnTimer = this.turnTimerCheckBox.isSelected() ? Integer.valueOf(this.turnTimerField.getText()): null;
		String localhost = "localhost";
		InetSocketAddress address = new InetSocketAddress(localhost, port);

		MLGModel config = new MLGModel();

		try {
			config.server = Server.CreateMLGServer(this.startScreen.getManager(), address, maxPlayers, turnTimer, this.serverName.getText()).get();
			config.client = new Client(localhost, port, "Host");
			config.client.connect().get();
		} catch (InterruptedException | ExecutionException | URISyntaxException e) {
			config.cleanUp();
			e.printStackTrace();
			return;
		}

		MLGLobby lobby = new MLGLobby(this.startScreen, config);
		this.startScreen.pushController(lobby);
	}

	private void cancelButtonAction(ActionEvent event) {
		this.startScreen.popController(this);
	}

}
