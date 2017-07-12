package de.prob2.ui.menu;

import java.io.File;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Injector;

import de.prob2.ui.MainController;
import de.prob2.ui.history.HistoryView;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.operations.OperationsView;
import de.prob2.ui.persistence.UIState;
import de.prob2.ui.project.ProjectView;
import de.prob2.ui.stats.StatsView;
import de.prob2.ui.verifications.VerificationsView;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Menu;
import javafx.stage.FileChooser;

public class PerspectivesMenu extends Menu {
	private static final Logger logger = LoggerFactory.getLogger(PerspectivesMenu.class);

	private final Injector injector;
	private final StageManager stageManager;

	@Inject
	private PerspectivesMenu(final StageManager stageManager, final Injector injector) {
		this.injector = injector;
		this.stageManager = stageManager;
		stageManager.loadFXML(this, "perspectivesMenu.fxml");
	}

	@FXML
	private void handleLoadDefault() {
		reset();
		loadPreset("main.fxml");
	}

	@FXML
	private void handleLoadSeparated() {
		reset();
		loadPreset("separatedHistory.fxml");
	}

	@FXML
	private void handleLoadSeparated2() {
		reset();
		loadPreset("separatedHistoryAndStatistics.fxml");
	}

	@FXML
	private void handleLoadDetached() {
		injector.getInstance(DetachViewStageController.class).showAndWait();
	}

	@FXML
	private void handleLoadPerspective() {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Open File");
		fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("FXML Files", "*.fxml"));
		File selectedFile = fileChooser.showOpenDialog(stageManager.getMainStage());
		if (selectedFile != null) {
			try {
				MainController main = injector.getInstance(MainController.class);
				FXMLLoader loader = injector.getInstance(FXMLLoader.class);
				loader.setLocation(selectedFile.toURI().toURL());
				injector.getInstance(UIState.class)
						.setGuiState("custom " + selectedFile.toURI().toURL().toExternalForm());
				reset();
				loader.setRoot(main);
				loader.setController(main);
				Parent root = loader.load();
				stageManager.getMainStage().getScene().setRoot(root);
			} catch (IOException e) {
				logger.error("Loading fxml failed", e);
				stageManager.makeAlert(Alert.AlertType.ERROR, "Could not open file:\n" + e).showAndWait();
			}
		}
	}

	private void reset() {
		injector.getInstance(UIState.class).clearDetachedStages();
		injector.getInstance(UIState.class).getExpandedTitledPanes().clear();
		injector.getInstance(DetachViewStageController.class).resetCheckboxes();
		injector.getInstance(OperationsView.class).setVisible(true);
		injector.getInstance(HistoryView.class).setVisible(true);
		injector.getInstance(StatsView.class).setVisible(true);
		injector.getInstance(VerificationsView.class).setVisible(true);
		injector.getInstance(ProjectView.class).setVisible(true);
	}

	public Parent loadPreset(String location) {
		injector.getInstance(UIState.class).setGuiState(location);
		final MainController root = injector.getInstance(MainController.class);
		root.refresh();
		stageManager.getMainStage().getScene().setRoot(root);		
		injector.getInstance(MenuController.class).setMacMenu();
		
		return root;
	}
}