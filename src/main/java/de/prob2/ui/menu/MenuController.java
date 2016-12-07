package de.prob2.ui.menu;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.prefs.Preferences;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.be4.classicalb.core.parser.exceptions.BException;

import de.codecentric.centerdevice.MenuToolkit;

import de.prob.scripting.Api;
import de.prob.statespace.AnimationSelector;
import de.prob.statespace.StateSpace;
import de.prob.statespace.Trace;

import de.prob2.ui.MainController;
import de.prob2.ui.animations.AnimationsView;
import de.prob2.ui.consoles.b.BConsoleStage;
import de.prob2.ui.consoles.groovy.GroovyConsoleStage;
import de.prob2.ui.formula.FormulaInputStage;
import de.prob2.ui.history.HistoryView;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.internal.UIState;
import de.prob2.ui.modelchecking.ModelcheckingController;
import de.prob2.ui.operations.OperationsView;
import de.prob2.ui.preferences.PreferencesStage;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.stats.StatsView;

import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TitledPane;
import javafx.scene.input.KeyCombination;
import javafx.stage.FileChooser;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.Window;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public final class MenuController extends MenuBar {
	private final class DetachViewStageController extends Stage {
		@FXML private CheckBox detachOperations;
		@FXML private CheckBox detachHistory;
		@FXML private CheckBox detachModelcheck;
		@FXML private CheckBox detachStats;
		@FXML private CheckBox detachAnimations;
		private final Preferences windowPrefs;
		private final Map<CheckBox, Class<? extends Parent>> checkBoxMap;
		private final Map<Class<? extends Parent>, CheckBox> checkBoxMapReverse;
		private List<TitledPane> panes;
		private List<Parent> detachables;
		
		private DetachViewStageController() {
			windowPrefs = Preferences.userNodeForPackage(MenuController.DetachViewStageController.class);
			checkBoxMap = new HashMap<>();
			checkBoxMapReverse = new HashMap<>();
			stageManager.loadFXML(this, "detachedPerspectivesChoice.fxml", this.getClass().getName());
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
		
		private void updateDetachables(final List<TitledPane> detachablePanes) {
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
	private static final URL FXML_ROOT;
	
	static {
		try {
			FXML_ROOT = new URL(MenuController.class.getResource("menu.fxml"), "..");
		} catch (MalformedURLException e) {
			throw new IllegalStateException(e);
		}
	}
	
	private static final Logger logger = LoggerFactory.getLogger(MenuController.class);

	private final Injector injector;
	private final Api api;
	private final AnimationSelector animationSelector;
	private final CurrentTrace currentTrace;
	private final RecentFiles recentFiles;
	private final StageManager stageManager;
	private final UIState uiState;
	private final DetachViewStageController dvController;
	
	private final Object openLock;
	private Window window;

	@FXML private Menu recentFilesMenu;
	@FXML private MenuItem recentFilesPlaceholder;
	@FXML private MenuItem clearRecentFiles;
	@FXML private Menu windowMenu;
	@FXML private MenuItem preferencesItem;
	@FXML private MenuItem enterFormulaForVisualization;
	@FXML private MenuItem aboutItem;
	
	@Inject
	private MenuController(
		final Injector injector,
		final Api api,
		final AnimationSelector animationSelector,
		final CurrentTrace currentTrace,
		final RecentFiles recentFiles,
		final StageManager stageManager,
		final UIState uiState
	) {
		this.injector = injector;
		this.api = api;
		this.animationSelector = animationSelector;
		this.currentTrace = currentTrace;
		this.recentFiles = recentFiles;
		this.stageManager = stageManager;
		this.uiState = uiState;
		
		this.openLock = new Object();

		stageManager.loadFXML(this, "menu.fxml");

		if (System.getProperty("os.name", "").toLowerCase().contains("mac")) {
			// Mac-specific menu stuff
			this.setUseSystemMenuBar(true);
			final MenuToolkit tk = MenuToolkit.toolkit();
			
			// Remove About menu item from Help
			aboutItem.getParentMenu().getItems().remove(aboutItem);
			aboutItem.setText("About ProB 2");
			
			// Remove Preferences menu item from Edit
			preferencesItem.getParentMenu().getItems().remove(preferencesItem);
			preferencesItem.setAccelerator(KeyCombination.valueOf("Shortcut+,"));
			
			// Create Mac-style application menu
			final Menu applicationMenu = tk.createDefaultApplicationMenu("ProB 2");
			this.getMenus().add(0, applicationMenu);
			tk.setApplicationMenu(applicationMenu);
			applicationMenu.getItems().setAll(aboutItem, new SeparatorMenuItem(), preferencesItem,
				new SeparatorMenuItem(), tk.createHideMenuItem("ProB 2"), tk.createHideOthersMenuItem(),
				tk.createUnhideAllMenuItem(), new SeparatorMenuItem(), tk.createQuitMenuItem("ProB 2"));
			
			// Add Mac-style items to Window menu
			windowMenu.getItems().addAll(tk.createMinimizeMenuItem(), tk.createZoomMenuItem(),
				tk.createCycleWindowsItem(), new SeparatorMenuItem(), tk.createBringAllToFrontItem(),
				new SeparatorMenuItem());
			tk.autoAddWindowMenuItems(windowMenu);
			
			// Make this the global menu bar
			tk.setGlobalMenuBar(this);
		}

		this.dvController = this.new DetachViewStageController();
	}
	
	@FXML
	public void initialize() {
		this.sceneProperty().addListener((observable, from, to) -> {
			if (to != null) {
				to.windowProperty().addListener((observable1, from1, to1) -> this.window = to1);
			}
		});
		
		final ListChangeListener<String> recentFilesListener = change -> {
			final ObservableList<MenuItem> recentItems = this.recentFilesMenu.getItems();
			final List<MenuItem> newItems = getRecentFileItems();

			// If there are no recent files, show a placeholder and disable clearing
			this.clearRecentFiles.setDisable(newItems.isEmpty());
			if (newItems.isEmpty()) {
				newItems.add(this.recentFilesPlaceholder);
			}
			
			// Add a shortcut for reopening the most recent file
			newItems.get(0).setAccelerator(KeyCombination.valueOf("Shift+Shortcut+'O'"));
			
			// Keep the last two items (the separator and the "clear recent files" item)
			newItems.addAll(recentItems.subList(recentItems.size()-2, recentItems.size()));
			
			// Replace the old recents with the new ones
			this.recentFilesMenu.getItems().setAll(newItems);
		};
		this.recentFiles.addListener(recentFilesListener);
		// Fire the listener once to populate the recent files menu
		recentFilesListener.onChanged(null);
		
		this.enterFormulaForVisualization.disableProperty().bind(currentTrace.currentStateProperty().initializedProperty().not());
	}
	
	@FXML
	private void handleClearRecentFiles() {
		this.recentFiles.clear();
	}

	@FXML
	private void handleLoadDefault() {
		uiState.clearDetachedStages();
		loadPreset("main.fxml");
	}

	@FXML
	private void handleLoadSeparated() {
		uiState.clearDetachedStages();
		loadPreset("separatedHistory.fxml");
	}

	@FXML
	private void handleLoadSeparated2() {
		uiState.clearDetachedStages();
		loadPreset("separatedHistoryAndStatistics.fxml");
	}

	@FXML
	private void handleLoadStacked() {
		uiState.clearDetachedStages();
		loadPreset("stackedLists.fxml");
	}

	@FXML
	public void handleLoadDetached() {
		this.dvController.show();
		this.dvController.toFront();
	}

	@FXML
	private void handleLoadPerspective() {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Open File");
		fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("FXML Files", "*.fxml"));
		File selectedFile = fileChooser.showOpenDialog(window);
		if (selectedFile != null) {
			FXMLLoader loader = injector.getInstance(FXMLLoader.class);
			Parent root;
			try {
				loader.setLocation(selectedFile.toURI().toURL());
				root = loader.load();
			} catch (IOException e) {
				logger.error("loading fxml failed", e);
				stageManager.makeAlert(Alert.AlertType.ERROR, "Could not open file:\n" + e).showAndWait();
				return;
			}
			window.getScene().setRoot(root);
			uiState.setGuiState(selectedFile.toString());
		}
	}
	
	private void openAsync(String path) {
		new Thread(() -> this.open(path), "File Opener Thread").start();
	}
	
	private void open(String path) {
		// NOTE: This method may be called from outside the JavaFX main thread, for example from openAsync.
		// This means that all JavaFX calls must be wrapped in Platform.runLater.
		
		// Prevent multiple threads from loading a file at the same time
		synchronized (this.openLock) {
			final StateSpace newSpace;
			try {
				newSpace = this.api.b_load(path);
			} catch (IOException | BException e) {
				logger.error("loading file failed", e);
				Platform.runLater(() -> stageManager.makeAlert(Alert.AlertType.ERROR, "Could not open file:\n" + e).show());
				return;
			}
			
			this.animationSelector.addNewAnimation(new Trace(newSpace));
			Platform.runLater(() -> {
				injector.getInstance(ModelcheckingController.class).resetView();
				
				// Remove the path first to avoid listing the same file twice.
				this.recentFiles.remove(path);
				this.recentFiles.add(0, path);
			});
		}
	}

	@FXML
	private void handleOpen(ActionEvent event) {
		final FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Open File");
		fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Classical B Files", "*.mch", "*.ref", "*.imp"));

		final File selectedFile = fileChooser.showOpenDialog(this.window);
		if (selectedFile == null) {
			return;
		}

		this.openAsync(selectedFile.getAbsolutePath());
	}

	@FXML
	private void handleClose(final ActionEvent event) {
		final Stage stage = this.stageManager.getCurrent();
		if (stage != null) {
			stage.close();
		}
	}

	@FXML
	private void handlePreferences() {
		final Stage preferencesStage = injector.getInstance(PreferencesStage.class);
		preferencesStage.show();
		preferencesStage.toFront();
	}

	@FXML
	private void handleFormulaInput() {
		final Stage formulaInputStage = injector.getInstance(FormulaInputStage.class);
		formulaInputStage.showAndWait();
		formulaInputStage.toFront();
	}

	@FXML
	private void handleGroovyConsole() {
		final Stage groovyConsoleStage = injector.getInstance(GroovyConsoleStage.class);
		groovyConsoleStage.show();
		groovyConsoleStage.toFront();
	}
	
	@FXML
	private void handleBConsole() {
		final Stage bConsoleStage = injector.getInstance(BConsoleStage.class);
		bConsoleStage.show();
		bConsoleStage.toFront();
	}
	
	public void detach(String id) {
		switch (id) {
			case "de.prob2.ui.operations.OperationsView":
				dvController.detachOperations.fire();
				break;
			case "de.prob2.ui.history.HistoryView":
				dvController.detachHistory.fire();
				break;
			case "de.prob2.ui.modelchecking.ModelcheckingController":
				dvController.detachModelcheck.fire();
				break;
			case "de.prob2.ui.stats.StatsView":
				dvController.detachStats.fire();
				break;
			case "de.prob2.ui.animations.AnimationsView":
				dvController.detachAnimations.fire();
				break;
			default:
				throw new IllegalArgumentException("Don't know how to detach " + id);
		}
	}

	public Parent loadPreset(String location) {
		FXMLLoader loader = injector.getInstance(FXMLLoader.class);
		this.uiState.setGuiState(location);
		injector.getInstance(MainController.class).refresh(uiState);
		try {
			loader.setLocation(new URL(FXML_ROOT, location));
		} catch (MalformedURLException e) {
			logger.error("Malformed location", e);
			stageManager.makeAlert(Alert.AlertType.ERROR, "Malformed location:\n" + e).showAndWait();
			return null;
		}
		loader.setRoot(injector.getInstance(MainController.class));
		
		final Parent root;
		final List<TitledPane> panes = new ArrayList<>();
		loader.setController(new Object() {
			@FXML private TitledPane operationsTP;
			@FXML private TitledPane historyTP;
			@FXML private TitledPane modelcheckTP;
			@FXML private TitledPane statsTP;
			@FXML private TitledPane animationsTP;
			
			@FXML
			public void initialize() {
				panes.add(operationsTP);
				panes.add(historyTP);
				panes.add(modelcheckTP);
				panes.add(statsTP);
				panes.add(animationsTP);
			}
		});
		try {
			root = loader.load();
		} catch (IOException e) {
			logger.error("loading fxml failed", e);
			stageManager.makeAlert(Alert.AlertType.ERROR, "Could not open file:\n" + e).showAndWait();
			return null;
		}
		window.getScene().setRoot(root);
		dvController.updateDetachables(panes);
		
		if (System.getProperty("os.name", "").toLowerCase().contains("mac")) {
			final MenuToolkit tk = MenuToolkit.toolkit();
			tk.setGlobalMenuBar(this);
			tk.setApplicationMenu(this.getMenus().get(0));
		}
		return root;
	}
	
	@FXML
	public void handleReportBug() {
		final Stage reportBugStage = injector.getInstance(ReportBugStage.class);
		reportBugStage.show();
		reportBugStage.toFront();
	}

	private List<MenuItem> getRecentFileItems(){
		final List<MenuItem> newItems = new ArrayList<>();
		for (String s : this.recentFiles) {
			final MenuItem item = new MenuItem(new File(s).getName());
			item.setOnAction(event -> this.openAsync(s));
			newItems.add(item);
		}
		return newItems;
	}
}
