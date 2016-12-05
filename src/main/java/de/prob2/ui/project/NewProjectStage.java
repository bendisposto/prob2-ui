package de.prob2.ui.project;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentStage;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.util.Pair;

@Singleton
public class NewProjectStage extends Stage {
	private static final Logger logger = LoggerFactory.getLogger(NewProjectStage.class);

	@FXML
	private Button finishButton;
	@FXML
	private TextField projectNameField;
	@FXML
	private TextField projectDescriptionField;
	@FXML
	private TextField locationField;
	@FXML
	private ListView<Preference> preferencesListView;
	@FXML
	private Label errorExplanationLabel;
	@FXML
	private TableView<Machine> machinesTableView;
	@FXML
	private TableColumn<Machine, String> nameColumn;
	@FXML
	private TableColumn<Machine, String> descriptionColumn;

	private CurrentProject currentProject;
	private CurrentStage currentStage;
	private Map<Pair<Integer, Integer>, Boolean> preferencesMap = new HashMap<>();

	private FXMLLoader loader;

	@Inject
	private NewProjectStage(FXMLLoader loader, CurrentProject currentProject, CurrentStage currentStage) {
		this.currentProject = currentProject;
		this.currentStage = currentStage;
		this.loader = loader;
		try {
			loader.setLocation(getClass().getResource("new_project_stage.fxml"));
			loader.setRoot(this);
			loader.setController(this);
			loader.load();
		} catch (IOException e) {
			logger.error("loading fxml failed", e);
		}
		this.initModality(Modality.WINDOW_MODAL);
		this.initOwner(currentStage.get());
		currentStage.register(this);
	}

	@FXML
	public void initialize() {
		finishButton.disableProperty().bind(projectNameField.lengthProperty().lessThanOrEqualTo(0));
		locationField.setText(System.getProperty("user.home"));

		nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
		descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
	}

	@FXML
	void addPreference(ActionEvent event) {
		AddProBPreferencesStage addProBPreferencesStage = new AddProBPreferencesStage(loader, currentStage);
		Preference preference = addProBPreferencesStage.showStage();
		if (preference != null) {
			preferencesListView.getItems().add(preference);

			TableColumn<Machine, Boolean> preferenceColumn = new TableColumn<>(preference.toString());

			preferenceColumn.setEditable(true);
			preferenceColumn.setCellFactory(new Callback<TableColumn<Machine, Boolean>, TableCell<Machine, Boolean>>() {
				public TableCell<Machine, Boolean> call(TableColumn<Machine, Boolean> p) {
					return new CheckBoxCell<Machine, Boolean>();
				}
			});
			machinesTableView.getColumns().add(preferenceColumn);
		}
	}

	private class CheckBoxCell<S, T> extends TableCell<S, T> {
		private final CheckBox checkBox;

		public CheckBoxCell() {
			this.checkBox = new CheckBox();
			checkBox.setOnAction((event) -> {
				int column = this.getTableView().getColumns().indexOf(this.getTableColumn());
				int row = this.getIndex();
				boolean checked = checkBox.isSelected();
				preferencesMap.put(new Pair<Integer, Integer>(row, column), checked);
			});
			this.checkBox.setAlignment(Pos.CENTER);

			setAlignment(Pos.CENTER);
			setGraphic(checkBox);
		}

		@Override
		public void updateItem(T item, boolean empty) {
			super.updateItem(item, empty);
			if (empty) {
				setText(null);
				setGraphic(null);
			} else {
				setGraphic(checkBox);
			}
		}
	}

	@FXML
	void addMachine(ActionEvent event) {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Add Machine");
		fileChooser.getExtensionFilters()
				.add(new FileChooser.ExtensionFilter("Classical B Files", "*.mch", "*.ref", "*.imp"));

		final File selectedFile = fileChooser.showOpenDialog(this);
		if (selectedFile == null) {
			return;
		}

		AddMachineStage addMachineStage = new AddMachineStage(loader, currentStage, selectedFile);
		Machine machine = addMachineStage.showStage();

		if (machine != null) {
			machinesTableView.getItems().add(machine);
		}
	}

	@FXML
	void selectLocation(ActionEvent event) {
		DirectoryChooser dirChooser = new DirectoryChooser();
		dirChooser.setTitle("Select Location");
		locationField.setText(dirChooser.showDialog(this.getOwner()).getAbsolutePath());
	}

	@FXML
	void cancel(ActionEvent event) {
		this.close();
	}

	@FXML
	void finish(ActionEvent event) {
		File dir = new File(locationField.getText());
		if (!dir.isDirectory()) {
			errorExplanationLabel.setText("The location does not exist or is invalid");
			return;
		}
		List<Machine> machines = machinesTableView.getItems();
		machines = copyMachines(machines, dir);
		List<Preference> preferences = preferencesListView.getItems();
		Project newProject = new Project(projectNameField.getText(), projectDescriptionField.getText(), machines,
				preferences, dir);
		currentProject.changeCurrentProject(newProject);
		currentProject.save();
		this.close();
	}

	private List<Machine> copyMachines(List<Machine> machines, File dir) {
		String path = dir.getAbsolutePath();
		for (Machine machine : machines) {
			int i = machines.indexOf(machine);
			Path source = machine.getLocation().toPath();
			File newLocation = new File(path + File.separator + machine.getLocation().getName());
			List<String> preferences = getSelectedPreferences(machine);
			Machine newMachine = new Machine(machine.getName(), machine.getDescription(), preferences, newLocation);
			try {
				Files.copy(source, newLocation.toPath(), StandardCopyOption.REPLACE_EXISTING);
				machines.set(i, newMachine);
			} catch (IOException e) {
				logger.error(
						"Could not copy file to the selected directory: " + machine.getLocation().getAbsolutePath(), e);
			}
		}
		return machines;
	}

	private List<String> getSelectedPreferences(Machine machine) {
		int totalColumns = machinesTableView.getColumns().size();
		int firstPrefColumn = totalColumns - preferencesListView.getItems().size();
		int row = machinesTableView.getItems().indexOf(machine);

		List<String> prefs = new ArrayList<>();
		for (int column = firstPrefColumn; column < totalColumns; column++) {
			if (preferencesMap.get(new Pair<>(row, column)) != null && preferencesMap.get(new Pair<>(row, column))) {
				Preference preference = preferencesListView.getItems().get(column - firstPrefColumn);
				prefs.add(preference.toString());
			}
		}
		return prefs;
	}
}
