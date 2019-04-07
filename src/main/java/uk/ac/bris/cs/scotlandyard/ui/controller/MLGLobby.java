package uk.ac.bris.cs.scotlandyard.ui.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import uk.ac.bris.cs.fxkit.BindFXML;
import uk.ac.bris.cs.fxkit.Controller;
import uk.ac.bris.cs.scotlandyard.ui.model.MLGProperty;

@BindFXML(value = "layout/MLGLobby.fxml", css = "style/mlg.css")
public final class MLGLobby implements Controller {

	@FXML private StackPane root;

	@Override
	public Parent root() {
		return this.root;
	}

	private MLGStartScreen startScreen;

	MLGLobby(MLGStartScreen startScreen, MLGProperty config) {
		this.startScreen = startScreen;
		Controller.bind(this);

	}



}
