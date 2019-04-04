package uk.ac.bris.cs.scotlandyard.ui.controller;

import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.layout.VBox;
import uk.ac.bris.cs.fxkit.BindFXML;
import uk.ac.bris.cs.fxkit.Controller;

@BindFXML(value = "layout/MLGJoinGame.fxml", css = "style/startscreen.css")
public final class MLGJoinGame implements Controller {

	@FXML private VBox root;

	MLGJoinGame() {
		Controller.bind(this);

	}

	@Override
	public Parent root() {
		return this.root;
	}

}
