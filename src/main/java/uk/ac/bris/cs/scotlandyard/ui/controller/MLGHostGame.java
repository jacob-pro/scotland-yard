package uk.ac.bris.cs.scotlandyard.ui.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import uk.ac.bris.cs.fxkit.BindFXML;
import uk.ac.bris.cs.fxkit.Controller;

@BindFXML(value = "layout/MLGHostGame.fxml", css = "style/mlg.css")
public final class MLGHostGame implements Controller {

	@FXML private StackPane root;
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
		this.hostButton.setOnAction(this::hostButtonAction);
		this.cancelButton.setOnAction(this::cancelButtonAction);
	}

	private void hostButtonAction(ActionEvent event) {

	}

	private void cancelButtonAction(ActionEvent event) {
		this.startScreen.popController(this);
	}



}
