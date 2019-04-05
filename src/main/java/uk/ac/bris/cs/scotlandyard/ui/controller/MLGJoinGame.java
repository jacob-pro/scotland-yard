package uk.ac.bris.cs.scotlandyard.ui.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import uk.ac.bris.cs.fxkit.BindFXML;
import uk.ac.bris.cs.fxkit.Controller;

@BindFXML(value = "layout/MLGJoinGame.fxml", css = "style/mlg.css")
public final class MLGJoinGame implements Controller {

	@FXML private StackPane root;
	@FXML private TextField addressField;
	@FXML private TextField portField;
	@FXML private Button joinButton;
	@FXML private Button cancelButton;

	private MLGStartScreen startScreen;

	MLGJoinGame(MLGStartScreen startScreen) {
		this.startScreen = startScreen;
		Controller.bind(this);
		this.joinButton.setOnAction(this::joinButtonAction);
		this.cancelButton.setOnAction(this::cancelButtonAction);
	}


	private void joinButtonAction(ActionEvent event) {

	}

	private void cancelButtonAction(ActionEvent event) {
		this.startScreen.popNode(this.root);
	}

	@Override
	public Parent root() {
		return this.root;
	}

}
