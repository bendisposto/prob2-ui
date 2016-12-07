package de.prob2.ui.menu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.prefs.Preferences;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob2.ui.animations.AnimationsView;
import de.prob2.ui.history.HistoryView;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.modelchecking.ModelcheckingController;
import de.prob2.ui.operations.OperationsView;
import de.prob2.ui.stats.StatsView;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TitledPane;
import javafx.stage.Screen;
import javafx.stage.Stage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public final class DetachViewStage extends Stage {
	private static final Logger LOGGER = LoggerFactory.getLogger(DetachViewStage.class);
	
	@FXML private CheckBox detachOperations;
	@FXML private CheckBox detachHistory;
	@FXML private CheckBox detachModelcheck;
	@FXML private CheckBox detachStats;
	@FXML private CheckBox detachAnimations;
	
	private final StageManager stageManager;
	private final Preferences windowPrefs;
	private final Map<CheckBox, Class<? extends Parent>> checkBoxMap;
	private final Map<Class<? extends Parent>, CheckBox> checkBoxMapReverse;
	private List<TitledPane> panes;
	private List<Parent> detachables;
	
	@Inject
	private DetachViewStage(final StageManager stageManager) {
		this.stageManager = stageManager;
		windowPrefs = Preferences.userNodeForPackage(DetachViewStage.class);
		checkBoxMap = new HashMap<>();
		checkBoxMapReverse = new HashMap<>();
		this.stageManager.loadFXML(this, "detachedPerspectivesChoice.fxml", this.getClass().getName());
	}

	@FXML
	public void initialize() {
		checkBoxMap.put(detachOperations, OperationsView.class);
		checkBoxMap.put(detachHistory, HistoryView.class);
		checkBoxMap.put(detachModelcheck, ModelcheckingController.class);
		checkBoxMap.put(detachStats, StatsView.class);
		checkBoxMap.put(detachAnimations, AnimationsView.class);
		for (final Map.Entry<CheckBox, Class<? extends Parent>> entry : checkBoxMap.entrySet()) {
			checkBoxMapReverse.put(entry.getValue(), entry.getKey());
		}
	}
	
	public void detach(String id) {
		final Class<? extends Parent> clazz;
		try {
			clazz = Class.forName(id).asSubclass(Parent.class);
		} catch (ClassNotFoundException e) {
			LOGGER.warn("Class not found for id, cannot detach", e);
			return;
		} catch (ClassCastException e) {
			LOGGER.warn("Class is not a Parent subclass, cannot detach", e);
			return;
		}
		
		final CheckBox checkBox = checkBoxMapReverse.get(clazz);
		if (checkBox == null) {
			LOGGER.warn("No checkbox found for {}, cannot detach", clazz);
			return;
		}
		checkBox.fire();
	}
	
	public void updateDetachables(final List<TitledPane> detachablePanes) {
		panes = new ArrayList<>(detachablePanes);
		detachables = new ArrayList<>();
		for (final TitledPane pane : panes) {
			detachables.add((Parent)pane.getContent());
		}
	}

	@FXML
	private void checkboxHandler(ActionEvent event) {
		final CheckBox s = (CheckBox)event.getSource();
		final Class<? extends Parent> clazz = checkBoxMap.get(s);
		
		for (int i = 0; i < panes.size(); i++) {
			final TitledPane tp = panes.get(i);
			final Parent detachable = detachables.get(i);
			if (clazz.isInstance(detachable)) {
				if (s.isSelected()) {
					transferToNewWindow(tp, tp.getText(), clazz.getName());
				} else {
					detachable.getScene().getWindow().hide();
				}
				return;
			}
		}
		
		throw new IllegalStateException("Didn't find " + clazz + " in any of the panes");
	}
	
	private Stage transferToNewWindow(TitledPane tp, String title, String id) {
		Parent node = (Parent)tp.getContent();
		// TODO Remove the TitledPane from the accordion and don't just hide it
		tp.setVisible(false);
		tp.setContent(new Label("I should be invisible.\n(this pane is detached)"));
		
		Stage stage = new Stage();
		stage.setTitle(title);
		stage.showingProperty().addListener((observable, from, to) -> {
			if (!to) {
				windowPrefs.putDouble(node.getClass()+"X",stage.getX());
				windowPrefs.putDouble(node.getClass()+"Y",stage.getY());
				windowPrefs.putDouble(node.getClass()+"Width",stage.getWidth());
				windowPrefs.putDouble(node.getClass()+"Height",stage.getHeight());
				checkBoxMapReverse.get(node.getClass()).setSelected(false);
				node.getScene().setRoot(new Label("I should be invisible.\n(this stage is closed)"));
				tp.setContent(node);
				tp.setVisible(true);
			}
		});
		stage.setWidth(windowPrefs.getDouble(node.getClass()+"Width",200));
		stage.setHeight(windowPrefs.getDouble(node.getClass()+"Height",100));
		stage.setX(windowPrefs.getDouble(node.getClass()+"X", Screen.getPrimary().getVisualBounds().getWidth()-stage.getWidth()/2));
		stage.setY(windowPrefs.getDouble(node.getClass()+"Y", Screen.getPrimary().getVisualBounds().getHeight()-stage.getHeight()/2));
		
		stage.setScene(new Scene(node));
		stageManager.register(stage, id);
		stage.show();
		return stage;
	}
}
