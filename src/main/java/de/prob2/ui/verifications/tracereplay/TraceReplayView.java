package de.prob2.ui.verifications.tracereplay;

import java.io.File;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.layout.FontSize;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.project.machines.Machine;
import javafx.collections.ListChangeListener;
import javafx.collections.SetChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;

@Singleton
public class TraceReplayView extends ScrollPane {

	@FXML
	private TableView<ReplayTraceItem> traceTableView;
	@FXML
	private TableColumn<ReplayTraceItem, FontAwesomeIconView> statusColumn;
	@FXML
	private TableColumn<ReplayTraceItem, String> nameColumn;
	@FXML
	private Button checkButton;
	@FXML
	private Button cancelButton;
	@FXML
	private Button loadTraceButton;

	private final StageManager stageManager;
	private final CurrentProject currentProject;
	private final TraceLoader traceLoader;
	private final TraceChecker traceChecker;
	private final Injector injector;
	private final ResourceBundle bundle;

	@Inject
	private TraceReplayView(final StageManager stageManager, final CurrentProject currentProject,
			final TraceLoader traceLoader, final TraceChecker traceChecker, final Injector injector,
			ResourceBundle bundle) {
		this.stageManager = stageManager;
		this.currentProject = currentProject;
		this.traceLoader = traceLoader;
		this.traceChecker = traceChecker;
		this.injector = injector;
		this.bundle = bundle;
		stageManager.loadFXML(this, "trace_replay_view.fxml");
	}

	@FXML
	private void initialize() {
		statusColumn.setCellValueFactory(new PropertyValueFactory<>("statusIcon"));
		statusColumn.setStyle("-fx-alignment: CENTER;");
		nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));

		currentProject.currentMachineProperty().addListener((observable, from, to) -> {
			updateTraceTableView(to);
			loadTraceButton.setDisable(to == null);
		});

		this.traceTableView.setRowFactory(param -> {

			final ContextMenu menu = new ContextMenu();
			final TableRow<ReplayTraceItem> row = new TableRow<>();
			row.setContextMenu(menu);
			//
			final MenuItem item = new MenuItem("Replay Trace"); // TODO: i18n
			menu.getItems().add(item);
			//
			item.setOnAction(event -> this.traceChecker.replayTrace(row.getItem().getTrace(), true));
			//
			return row;
		});

		FontSize fontsize = injector.getInstance(FontSize.class);
		((FontAwesomeIconView) (checkButton.getGraphic())).glyphSizeProperty().bind(fontsize.multiply(2.0));
		((FontAwesomeIconView) (cancelButton.getGraphic())).glyphSizeProperty().bind(fontsize.multiply(2.0));
		statusColumn.minWidthProperty().bind(fontsize.multiply(6.0));
		statusColumn.maxWidthProperty().bind(fontsize.multiply(6.0));

		traceChecker.currentJobThreadsProperty()
				.addListener((observable, from, to) -> cancelButton.setDisable(to.isEmpty()));
		traceTableView.itemsProperty().get()
				.addListener((ListChangeListener<ReplayTraceItem>) c -> checkButton.setDisable(c.getList().isEmpty()));
	}

	private void updateTraceTableView(Machine machine) {
		traceTableView.getItems().clear();
		if (machine != null) {
			addToTraceTableView(machine.getTraces());
			machine.getTraces().addListener((SetChangeListener<File>) c -> {
				if (c.wasAdded()) {
					addToTraceTableView(c.getElementAdded());
				}
				if (c.wasRemoved()) {
					removeFromTraceTableView(c.getElementRemoved());
				}
			});
		}
	}

	private void addToTraceTableView(Set<? extends File> traceFiles) {
		for (File traceFile : traceFiles) {
			addToTraceTableView(traceFile);
		}
	}

	private void addToTraceTableView(File traceFile) {
		ReplayTrace trace = traceLoader.loadTrace(traceFile);
		traceTableView.getItems().add(new ReplayTraceItem(trace, traceFile));
	}

	private void removeFromTraceTableView(File traceFile) {
		traceTableView.getItems().stream().filter(traceItem -> !traceFile.equals(traceItem.getLocation()))
				.collect(Collectors.toList());
	}

	@FXML
	private void checkMachine() {
		traceChecker.checkMachine(traceTableView.getItems());
	}

	@FXML
	public synchronized void cancel() {
		traceChecker.cancelReplay();
	}

	@FXML
	private void loadTraceFromFile() {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle(bundle.getString("verifications.tracereplay.traceLoader.dialog.title"));
		fileChooser.setInitialDirectory(currentProject.getLocation());
		fileChooser.getExtensionFilters().add(new ExtensionFilter("Trace (*.trace)", "*.trace"));
		File traceFile = fileChooser.showOpenDialog(stageManager.getCurrent());
		currentProject.getCurrentMachine().addTrace(traceFile);
	}
}
