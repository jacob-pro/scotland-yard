package uk.ac.bris.cs.scotlandyard.ui.controller;

import com.google.gson.Gson;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.util.StringConverter;
import uk.ac.bris.cs.fxkit.BindFXML;
import uk.ac.bris.cs.fxkit.Controller;
import uk.ac.bris.cs.scotlandyard.model.Colour;
import uk.ac.bris.cs.scotlandyard.multiplayer.Observer;
import uk.ac.bris.cs.scotlandyard.multiplayer.model.Join;
import uk.ac.bris.cs.scotlandyard.multiplayer.model.Lobby;
import uk.ac.bris.cs.scotlandyard.multiplayer.model.LobbyPlayer;
import uk.ac.bris.cs.scotlandyard.ui.Utils;
import uk.ac.bris.cs.scotlandyard.ui.model.MLGModel;

import java.util.ArrayList;
import java.util.List;

@BindFXML(value = "layout/MLGLobby.fxml", css = "style/mlg.css")
public final class MLGLobby implements Controller, Observer {

	@FXML private StackPane root;
	@FXML private Label serverNameLabel;
	@FXML private Label maxPlayersLabel;
	@FXML private Label turnTimerLabel;
	@FXML private ToggleButton readyButton;
	@FXML private Button exitButton;
	@FXML private TableView<LobbyPlayer> tableView;
	@FXML private TableColumn<LobbyPlayer, String> nameColumn;
	@FXML private TableColumn<LobbyPlayer, String> colourColumn;
	@FXML private TableColumn<LobbyPlayer, String> readyColumn;
	@FXML private ChoiceBox<Colour> colourChoice;

	@Override
	public Parent root() {
		return this.root;
	}

	private MLGStartScreen startScreen;
	private MLGModel config;
	private int playerID;

	MLGLobby(MLGStartScreen startScreen, MLGModel config) {
		this.startScreen = startScreen;
		this.config = config;
		Controller.bind(this);
		config.client.registerObserver(this);	//We need to sign up for lobby change and game start notifications

		Join joinMessage = config.client.joinMessage();
		this.serverNameLabel.setText("Server name: " + joinMessage.serverName);
		this.maxPlayersLabel.setText("Max players: " + joinMessage.maxPlayers.toString());
		this.turnTimerLabel.setText("Turn timer: " + (joinMessage.turnTimer == null ? "Disabled" : joinMessage.turnTimer.toString() + " seconds"));
		this.playerID = joinMessage.playerID;

		this.readyButton.setOnAction(this::readyButtonAction);
		this.exitButton.setOnAction(this::exitButtonAction);
		this.colourChoice.setOnAction(this::colourChangeAction);

		//Configure the table view
		this.tableView.setItems(FXCollections.observableArrayList());
		this.nameColumn.setCellValueFactory(p -> new ReadOnlyStringWrapper(p.getValue().name));
		this.colourColumn.setCellValueFactory(p -> new ReadOnlyStringWrapper(p.getValue().colour == null ? "Undecided" : p.getValue().colour.toString()));
		this.readyColumn.setCellValueFactory(p -> new ReadOnlyStringWrapper(p.getValue().ready ? "Yes" : "No"));

		//Configure colour picker
		this.colourChoice.setConverter(new StringConverter<Colour>() {
			@Override
			public String toString(Colour colour) {
				return (colour == null ? "Undecided" : colour.toString());
			}
			@Override
			public Colour fromString(String s) {
				return null;
			}
		});
		List<Colour> items = new ArrayList<>();
		items.add(null);	//Add the undecided choice
		this.colourChoice.setItems(FXCollections.observableList(items));

		//Fetch the lobby
		this.config.client.getLobby().whenComplete((result, error) -> {
			Platform.runLater(() -> {
				if (error == null) {
					this.onLobbyChange(result);
				}
			});
		});
	}

	@Override
	public void onLobbyChange(Lobby lobby) {
		LobbyPlayer us = lobby.players.stream().filter(p -> p.id == this.playerID).findFirst().orElseThrow();
		this.readyButton.setSelected(us.ready);
		List<Colour> taken = lobby.takenColours();
		taken.remove(us.colour);
		this.colourChoice.getItems().removeAll(taken);
		lobby.availableColours().forEach(c -> {
			if (!this.colourChoice.getItems().contains(c)) this.colourChoice.getItems().add(c);
		});
		this.tableView.getItems().setAll(lobby.players);

		Gson gson = new Gson();
		System.out.println(gson.toJson(lobby));
	}

	@Override
	public void onGameStarted() {
		this.startScreen.callBack.accept(this.config);
	}

	@Override
	public void onClientError(RuntimeException e) {
		Utils.handleFatalException(e);
	}

	private void exitButtonAction(ActionEvent event) {
		this.config.cleanUp();
		this.startScreen.popController(this);
	}

	private void readyButtonAction(ActionEvent event) {
		this.colourChoice.setDisable(this.readyButton.isSelected());
		this.config.client.setReady(this.readyButton.isSelected());
	}

	private void colourChangeAction(ActionEvent event) {
		Colour colour = this.colourChoice.getValue();
		this.config.client.setColour(colour);
		this.readyButton.setDisable(colour == null);
	}

}
