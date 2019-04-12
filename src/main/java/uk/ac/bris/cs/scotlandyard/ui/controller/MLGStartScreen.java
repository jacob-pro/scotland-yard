package uk.ac.bris.cs.scotlandyard.ui.controller;

import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.event.ActionEvent;
import javafx.scene.layout.StackPane;
import uk.ac.bris.cs.fxkit.BindFXML;
import uk.ac.bris.cs.fxkit.Controller;
import uk.ac.bris.cs.scotlandyard.ResourceManager;
import uk.ac.bris.cs.scotlandyard.ui.model.MLGProperty;

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

	private ResourceManager manager;

	MLGStartScreen(ResourceManager manager, Consumer<MLGProperty> callBack) {
		this.manager = manager;
		Controller.bind(this);
		this.joinButton.setOnAction(this::joinButtonAction);
		this.hostButton.setOnAction(this::hostButtonAction);
	}

	void popController(Controller controller) {
		this.stackPane.getChildren().remove(controller.root());
	}

	void pushController(Controller controller) {
		this.stackPane.getChildren().add(controller.root());
	}

	private void joinButtonAction(ActionEvent event) {
		JoinGameController join = new JoinGameController(this);
		this.pushController(join);
	}

	private void hostButtonAction(ActionEvent event) {
		HostGameController host = new HostGameController(this);
		this.pushController(host);
	}

	ResourceManager getManager() {
		return this.manager;
	}

}
