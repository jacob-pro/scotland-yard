package uk.ac.bris.cs.scotlandyard.ui.controller;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.event.ActionEvent;
import javafx.scene.layout.StackPane;
import uk.ac.bris.cs.fxkit.BindFXML;
import uk.ac.bris.cs.fxkit.Controller;
import uk.ac.bris.cs.scotlandyard.ResourceManager;
import uk.ac.bris.cs.scotlandyard.ui.model.BoardProperty;
import uk.ac.bris.cs.scotlandyard.ui.model.ModelProperty;

import java.util.function.Consumer;

@BindFXML(value = "layout/MLGStartScreen.fxml", css = "style/mlg.css")
public final class MLGStartScreen implements Controller {

	@FXML private StackPane stackPane;
	@FXML private Button joinButton;
	@FXML private Button hostButton;

	@Override
	public Parent root() {
		return this.stackPane;
	}

	MLGStartScreen(ResourceManager manager, BoardProperty config, Consumer<ModelProperty> consumer) {
		Controller.bind(this);
		this.joinButton.setOnAction(this::joinButtonAction);
		this.hostButton.setOnAction(this::hostButtonAction);
	}

	void popNode(Node node) {
		this.stackPane.getChildren().remove(node);
	}

	private void joinButtonAction(ActionEvent event) {
		MLGJoinGame join = new MLGJoinGame(this);
		this.stackPane.getChildren().add(join.root());
	}

	private void hostButtonAction(ActionEvent event) {

	}

}
