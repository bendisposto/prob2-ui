package de.prob2.ui;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Guice;
import com.google.inject.Injector;

import de.prob.cli.ProBInstanceProvider;
import de.prob2.ui.config.Config;
import de.prob2.ui.internal.ProB2Module;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.persistence.UIPersistence;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;

public class ProB2 extends Application {
	private static final Logger LOGGER = LoggerFactory.getLogger(ProB2.class);

	private Injector injector;
	private Config config;

	private Stage primaryStage;

	public static void main(String... args) {
		launch(args);
	}

	private void updateTitle() {
		final CurrentProject currentProject = injector.getInstance(CurrentProject.class);
		final CurrentTrace currentTrace = injector.getInstance(CurrentTrace.class);

		final StringBuilder title = new StringBuilder();
		if (currentTrace.exists()) {
			title.append(currentTrace.getModel().getModelFile().getName());
			title.append(" - ");
		}
		if (currentProject.exists()) {
			title.append(currentProject.getName());
			title.append(" - ");
		}
		title.append("ProB 2.0");
		if (!currentProject.isSaved()) {
			title.append("*");
		}

		this.primaryStage.setTitle(title.toString());
	}

	@Override
	public void start(Stage primaryStage) {
		this.primaryStage = primaryStage;
		ProB2Module module = new ProB2Module();
		injector = Guice.createInjector(com.google.inject.Stage.PRODUCTION, module);

		StageManager stageManager = injector.getInstance(StageManager.class);
		Thread.setDefaultUncaughtExceptionHandler((thread, exc) -> {
			LOGGER.error("Uncaught exception on thread {}", thread, exc);
			Platform.runLater(() -> {
				final Alert alert = stageManager.makeAlert(Alert.AlertType.ERROR);
				alert.setHeaderText("Uncaught internal exception");
				alert.setContentText(String.format(
						"An internal exception occurred and was not caught. This is probably a bug.%n%nException: %s%nThread: %s%n%nThe full stack trace can be found in the log file.",
						exc, thread));
				alert.show();
			});
		});

		config = injector.getInstance(Config.class);
		UIPersistence uiPersistence = injector.getInstance(UIPersistence.class);
		Parent root = injector.getInstance(MainController.class);
		Scene mainScene = new Scene(root, 1024, 768);
		primaryStage.setScene(mainScene);

		CurrentProject currentProject = injector.getInstance(CurrentProject.class);
		currentProject.addListener((observable, from, to) -> this.updateTitle());
		currentProject.savedProperty().addListener((observable, from, to) -> this.updateTitle());
		CurrentTrace currentTrace = injector.getInstance(CurrentTrace.class);
		currentTrace.addListener((observable, from, to) -> this.updateTitle());
		this.updateTitle();

		stageManager.register(primaryStage, this.getClass().getName());

		primaryStage.setOnCloseRequest(event -> {
			if (!currentProject.isSaved()) {
				ButtonType save = new ButtonType("Save", ButtonBar.ButtonData.YES);
				ButtonType doNotSave = new ButtonType("Do not save", ButtonBar.ButtonData.NO);
				Alert alert = stageManager.makeAlert(Alert.AlertType.CONFIRMATION,
						"The current project \"" + currentProject.getName()
								+ "\" contains unsaved changes.\nDo you want to save the project?",
						save, ButtonType.CANCEL, doNotSave);
				Optional<ButtonType> result = alert.showAndWait();
				if (result.isPresent() && result.get().equals(ButtonType.CANCEL)) {
					event.consume();
				} else if (result.isPresent() && result.get().equals(save)) {
					currentProject.save();
					Platform.exit();
				}
			} else {
				Platform.exit();
			}
		});

		primaryStage.show();
		uiPersistence.open();
	}

	@Override
	public void stop() {
		if (config != null) {
			config.save();
		}
		if (injector != null) {
			injector.getInstance(ProBInstanceProvider.class).shutdownAll();
		}
	}
}
