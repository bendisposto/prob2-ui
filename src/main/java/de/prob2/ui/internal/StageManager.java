package de.prob2.ui.internal;

import java.io.IOException;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tracks registered stages to implement UI persistence and the Mac Cmd+W shortcut. Also provides some convenience methods for creating {@link Stage}s and {@link Alert}s and loading FXML files.
 */
@Singleton
public final class StageManager {
	private static final Logger LOGGER = LoggerFactory.getLogger(StageManager.class);
	private static final String STYLESHEET = "prob.css";
	
	private final Injector injector;
	private final UIState uiState;
	
	private final ObjectProperty<Stage> current;
	
	@Inject
	private StageManager(final Injector injector, final UIState uiState) {
		this.injector = injector;
		this.uiState = uiState;
		
		this.current = new SimpleObjectProperty<>(this, "current");
	}
	
	/**
	 * Get a new FXMLLoader from the manager's injector.
	 * 
	 * @return a new FXMLLoader
	 */
	public FXMLLoader makeFXMLLoader() {
		return injector.getInstance(FXMLLoader.class);
	}
	
	/**
	 * Load a FXML file with {@code controller} as the root and controller. {@code filename} may be absolute, or relative to {@code controller}'s package.
	 * 
	 * @param controller the root and controller to use
	 * @param filename the FXML file to load
	 */
	public void loadFXML(final Object controller, final String filename) {
		final FXMLLoader loader = this.makeFXMLLoader();
		loader.setLocation(controller.getClass().getResource(filename));
		loader.setRoot(controller);
		loader.setController(controller);
		try {
			loader.load();
		} catch (IOException e) {
			LOGGER.error("Loading fxml failed", e);
		}
	}
	
	/**
	 * Load a FXML file using {@link #loadFXML(Object, String)} and register its root/controller using {@link #register(Stage)}.
	 *
	 * @param controller the root and controller to use
	 * @param filename the FXML file to load
	 */
	public void loadFXML(final Stage controller, final String filename) {
		this.loadFXML((Object)controller, filename);
		this.register(controller);
	}
	
	/**
	 * Register the given stage with the manager. The stage must already have its scene {@link Stage#setScene(Scene) set}. This method applies the {@link #STYLESHEET} to the stage's scene, sets the stage icon, and adds some internal listeners for implementing UI persistence and the Mac Cmd+W shortcut.
	 * 
	 * @param stage the stage to register
	 */
	public void register(final Stage stage) {
		stage.getScene().getStylesheets().add(STYLESHEET);
		stage.getIcons().add(new Image("prob_128.gif"));
		stage.showingProperty().addListener((observable, from, to) -> {
			if (to) {
				uiState.addStage(stage.getTitle());
			} else {
				uiState.removeStage(stage.getTitle());
			}
		});
		stage.focusedProperty().addListener((observable, from, to) -> {
			if (to) {
				this.current.set(stage);
			} else if (stage.equals(this.current.get())) {
				this.current.set(null);
			}
		});
	}
	
	/**
	 * Create with the given {@link Scene} as its scene, and register it automatically.
	 * 
	 * @param scene the new scene's stage
	 * @return a new stage with the given scene
	 */
	public Stage makeStage(final Scene scene) {
		final Stage stage = new Stage();
		stage.setScene(scene);
		this.register(stage);
		return stage;
	}
	
	/**
	 * Register the given dialog with the manager. Currently this only applies the {@link #STYLESHEET} to the dialog's dialog pane.
	 * 
	 * @param dialog the dialog to register
	 */
	public void register(final Dialog<?> dialog) {
		dialog.getDialogPane().getStylesheets().add(STYLESHEET);
	}
	
	/**
	 * Create and register a new alert. The arguments are the same as with {@link Alert#Alert(Alert.AlertType, String, ButtonType...)}.
	 * 
	 * @return a new alert
	 */
	@SuppressWarnings("OverloadedVarargsMethod") // OK here, because the overload is shorter than the vararg version
	public Alert makeAlert(final Alert.AlertType alertType, final String contentText, final ButtonType... buttons) {
		final Alert alert = new Alert(alertType, contentText, buttons);
		this.register(alert);
		return alert;
	}
	
	/**
	 * Create and register a new alert.
	 * 
	 * @param alertType the alert type
	 * @return a new alert
	 */
	public Alert makeAlert(final Alert.AlertType alertType) {
		final Alert alert = new Alert(alertType);
		this.register(alert);
		return alert;
	}
	
	/**
	 * A read-only property containing the currently focused stage. If a non-JavaFX window or an unregistered stage is in focus, the property's value is {@code null}.
	 * 
	 * @return a property containing the currently focused stage
	 */
	public ReadOnlyObjectProperty<Stage> currentProperty() {
		return this.current;
	}
	
	/**
	 * Get the currently focused stage. If a non-JavaFX window or an unregistered stage is in focus, this method returns {@code null}.
	 *
	 * @return the currently focused stage
	 */
	public Stage getCurrent() {
		return this.currentProperty().get();
	}
}
