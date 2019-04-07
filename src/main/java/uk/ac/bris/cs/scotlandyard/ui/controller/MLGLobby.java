package uk.ac.bris.cs.scotlandyard.ui.controller;

import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.layout.StackPane;
import uk.ac.bris.cs.fxkit.BindFXML;
import uk.ac.bris.cs.fxkit.Controller;
import uk.ac.bris.cs.scotlandyard.server.MLGObserver;
import uk.ac.bris.cs.scotlandyard.ui.model.MLGProperty;

@BindFXML(value = "layout/MLGLobby.fxml", css = "style/mlg.css")
public final class MLGLobby implements Controller, MLGObserver {

	@FXML private StackPane root;

	@Override
	public Parent root() {
		return this.root;
	}

	private MLGStartScreen startScreen;
	private MLGProperty config;

	MLGLobby(MLGStartScreen startScreen, MLGProperty config) {
		this.startScreen = startScreen;
		this.config = config;
		Controller.bind(this);
		config.client().registerObserver(this);
	}

	@Override
	public void onLobbyReceived() {

	}


}
