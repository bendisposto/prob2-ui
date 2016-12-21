package de.prob2.ui.project;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.prob2.ui.prob2fx.CurrentStage;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class AddProBPreferencesStage extends Stage {
	private static final Logger logger = LoggerFactory.getLogger(AddProBPreferencesStage.class);

	@FXML
	private Button finishButton;
	@FXML
	private Button addPreferenceButton;
	@FXML
	private TextField nameField;
	@FXML
	private TextField preferenceNameField;
	@FXML
	private TextField preferenceValueField;
	@FXML
	private ListView<Map.Entry<String, String>> preferencesListView;
	@FXML
	private Label errorExplanationLabel;

	private Map<String, String> preferenceMap = new HashMap<>();

	private Preference preference;

	private Set<String> preferencesSet;

	AddProBPreferencesStage(FXMLLoader loader, CurrentStage currentStage) {
		try {
			loader.setLocation(getClass().getResource("add_probpreferences_stage.fxml"));
			loader.setRoot(this);
			loader.setController(this);
			loader.load();
		} catch (IOException e) {
			logger.error("loading fxml failed", e);
		}
		this.initModality(Modality.WINDOW_MODAL);
		this.initOwner(currentStage.get());
		currentStage.register(this, this.getClass().getName());
	}

	@FXML
	public void initialize() {
		addPreferenceButton.disableProperty().bind(preferenceNameField.lengthProperty().lessThanOrEqualTo(0)
				.or(preferenceValueField.lengthProperty().lessThanOrEqualTo(0)));
		nameField.textProperty().addListener((observable, from, to) -> {
			if (preferencesSet.contains(to)) {
				finishButton.setDisable(true);
				errorExplanationLabel.setText("There is already a preference named '" + to + "'");
			} else if (to.equals("")) {
				finishButton.setDisable(true);
				errorExplanationLabel.setText("");
			} else {
				finishButton.setDisable(false);
				errorExplanationLabel.setText("");
			}
		});
	}

	@FXML
	void addPreference(ActionEvent event) {
		preferenceMap.put(preferenceNameField.getText(), preferenceValueField.getText());
		preferencesListView.getItems().clear();
		preferencesListView.getItems().addAll(preferenceMap.entrySet());
	}

	@FXML
	void cancel(ActionEvent event) {
		this.close();
	}

	@FXML
	void finish(ActionEvent event) {
		preference = new Preference(nameField.getText(), preferenceMap);
		this.close();
	}

	public Preference showStage(Set<String> preferencesList) {
		this.preferencesSet = preferencesList;
		super.showAndWait();
		return preference;
	}
}
