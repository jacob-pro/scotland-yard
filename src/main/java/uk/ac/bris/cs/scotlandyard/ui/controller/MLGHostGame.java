package uk.ac.bris.cs.scotlandyard.ui.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import uk.ac.bris.cs.fxkit.BindFXML;
import uk.ac.bris.cs.fxkit.Controller;
import uk.ac.bris.cs.scotlandyard.model.Colour;

@BindFXML(value = "layout/MLGHostGame.fxml", css = "style/mlg.css")
public final class MLGHostGame implements Controller {

	@FXML private StackPane root;
	@FXML private ChoiceBox<Colour> colourChoiceBox;
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
		this.turnTimerCheckBox.setOnAction(a -> {
			this.turnTimerField.setDisable(!this.turnTimerCheckBox.isSelected());
			this.turnTimerField.setText(null);
		});
		this.hostButton.setOnAction(this::hostButtonAction);
		this.cancelButton.setOnAction(this::cancelButtonAction);
		this.colourChoiceBox.getItems().setAll(Colour.values());

	}

	private void hostButtonAction(ActionEvent event) {



	}

	private void cancelButtonAction(ActionEvent event) {
		this.startScreen.popController(this);
	}



}
