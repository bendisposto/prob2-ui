package de.prob2.ui.menu;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob2.ui.internal.StageManager;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Accordion;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TitledPane;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.scene.layout.StackPane;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public final class DetachViewStage extends Stage {
	private static final Logger LOGGER = LoggerFactory.getLogger(DetachViewStage.class);
	
	@FXML private ListView<TitledPane> lv;
	
	private final StageManager stageManager;
	private final Preferences windowPrefs;
	private Accordion accordion;
	private List<TitledPane> panes;
	private List<Parent> detachables;
	
	@Inject
	private DetachViewStage(final StageManager stageManager) {
		this.stageManager = stageManager;
		windowPrefs = Preferences.userNodeForPackage(DetachViewStage.class);
		this.stageManager.loadFXML(this, "detachedPerspectivesChoice.fxml", this.getClass().getName());
	}

	@FXML
	public void initialize() {
		lv.setCellFactory(CheckBoxListCell.forListView(tp -> {
			final String id = detachables.get(panes.indexOf(tp)).getClass().getName();
			final BooleanProperty prop = new SimpleBooleanProperty(tp.getContent() == null);
			
			prop.addListener((observable, from, to) -> {
				if (to) {
					this.detach(id);
				} else {
					this.attach(id);
				}
			});
			
			tp.contentProperty().addListener((observable, from, to) -> prop.set(to == null));
			
			return prop;
		}, new StringConverter<TitledPane>() {
			@Override
			public String toString(final TitledPane object) {
				return object.getText();
			}
			
			@Override
			public TitledPane fromString(final String string) {
				throw new UnsupportedOperationException("Converting back to TitledPane is not supported");
			}
		}));
	}
	
	public void updateAccordion(final Accordion accordion) {
		this.accordion = accordion;
		this.panes = new ArrayList<>(this.accordion.getPanes());
		this.detachables = new ArrayList<>();
		for (final TitledPane pane : this.accordion.getPanes()) {
			this.detachables.add((Parent)pane.getContent());
		}
		lv.getItems().setAll(panes);
	}
	
	public void detach(String id) {
		for (Iterator<TitledPane> it = this.accordion.getPanes().iterator(); it.hasNext();) {
			final TitledPane pane = it.next();
			if (pane.getContent().getClass().getName().equals(id)) {
				it.remove();
				transferToNewWindow(pane, pane.getContent().getClass().getName());
				return;
			}
		}
	}
	
	public void attach(String id) {
		for (final Parent detachable : this.detachables) {
			if (detachable.getClass().getName().equals(id)) {
				final Scene scene = detachable.getScene();
				if (scene != null) {
					scene.getWindow().hide();
				}
			}
		}
	}
	
	private Stage transferToNewWindow(TitledPane tp, String id) {
		Parent node = (Parent)tp.getContent();
		tp.setContent(null);
		
		Stage stage = new Stage();
		stage.setTitle(tp.getText());
		stage.setOnHidden(event -> {
			windowPrefs.putDouble(node.getClass()+"X",stage.getX());
			windowPrefs.putDouble(node.getClass()+"Y",stage.getY());
			windowPrefs.putDouble(node.getClass()+"Width",stage.getWidth());
			windowPrefs.putDouble(node.getClass()+"Height",stage.getHeight());
			node.getScene().setRoot(new Label("I should be invisible.\n(this stage is closed)"));
			tp.setContent(node);
			// Only re-add panes that aren't detached
			accordion.getPanes().setAll(this.panes.stream().filter(p -> p.getContent() != null).collect(Collectors.toList()));
		});
		stage.setWidth(windowPrefs.getDouble(node.getClass()+"Width",200));
		stage.setHeight(windowPrefs.getDouble(node.getClass()+"Height",100));
		stage.setX(windowPrefs.getDouble(node.getClass()+"X", Screen.getPrimary().getVisualBounds().getWidth()-stage.getWidth()/2));
		stage.setY(windowPrefs.getDouble(node.getClass()+"Y", Screen.getPrimary().getVisualBounds().getHeight()-stage.getHeight()/2));
		
		stage.setScene(new Scene(new StackPane(node)));
		stageManager.register(stage, id);
		stage.show();
		// Necessary because Accordion sets all pane contents as not visible unless they are expanded.
		node.setVisible(true);
		return stage;
	}
}
