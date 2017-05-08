package de.prob2.ui.verifications.ltl;


import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.be4.ltl.core.parser.LtlParseException;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import de.prob.animator.command.EvaluationCommand;
import de.prob.animator.domainobjects.AbstractEvalResult;
import de.prob.animator.domainobjects.LTL;
import de.prob.check.LTLCounterExample;
import de.prob.check.LTLError;
import de.prob.check.LTLOk;
import de.prob.statespace.AnimationSelector;
import de.prob.statespace.State;
import de.prob.statespace.Trace;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.project.Project;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;

@Singleton
public class LTLView extends AnchorPane{
	
	public enum Checked {
		SUCCESS, FAIL;
	}
	
	private class LTLResultItem {
		
		private AlertType type;
		private Checked checked;
		private String message;
		private String header;
		private String exceptionText;
		private boolean isParseError;
		
		private LTLResultItem(AlertType type, Checked checked, String message, String header) {
			this.type = type;
			this.checked = checked;
			this.message = message;
			this.header = header;
		}
		
		private LTLResultItem(AlertType type, Checked checked, String message, String header, 
								String exceptionText, boolean isParseError) {
			this(type, checked, message, header);
			this.exceptionText = exceptionText;
			this.isParseError = true;
		}
	}
	
	private static final Logger logger = LoggerFactory.getLogger(LTLView.class);
	
	@FXML
	private TableView<LTLFormulaItem> tvFormula;
	
	@FXML
	private Button addLTLButton;
	
	@FXML
	private Button checkAllButton;
	
	@FXML
	private TableColumn<LTLFormulaItem, FontAwesomeIconView> statusColumn;
	
	@FXML
	private TableColumn<LTLFormulaItem, String> nameColumn;
	
	@FXML
	private TableColumn<LTLFormulaItem, String> descriptionColumn;
	
	@FXML
	private TableView<Machine> tvMachines;
	
	@FXML
	private TableColumn<LTLFormulaItem, FontAwesomeIconView> machineStatusColumn;
	
	@FXML
	private TableColumn<LTLFormulaItem, String> machineNameColumn;	
	
	private final Injector injector;
	
	private final CurrentTrace currentTrace;
	
	private final CurrentProject currentProject;
	
	private final AnimationSelector animations;
		
	@Inject
	private LTLView(final StageManager stageManager, final Injector injector, final AnimationSelector animations,
					final CurrentTrace currentTrace, final CurrentProject currentProject) {
		this.injector = injector;
		this.currentTrace = currentTrace;
		this.currentProject = currentProject;
		this.animations = animations;
		stageManager.loadFXML(this, "ltl_view.fxml");
	}
	
	@FXML
	public void initialize() {		
		tvFormula.setOnMouseClicked(e-> {
			LTLFormulaItem item = tvFormula.getSelectionModel().getSelectedItem();
			if(e.getClickCount() == 2 &&  item != null) {
				showCurrentItemDialog(item);
			}
		});
								
		tvFormula.setRowFactory(table -> {
			final TableRow<LTLFormulaItem> row = new TableRow<>();
			MenuItem removeItem = new MenuItem("Remove formula");
			removeItem.setOnAction(e -> {
				Machine machine = tvMachines.getFocusModel().getFocusedItem();
				LTLFormulaItem item = tvFormula.getSelectionModel().getSelectedItem();
				machine.removeLTLFormula(item);
				currentProject.update(new Project(currentProject.getName(), currentProject.getDescription(), 
						currentProject.getMachines(), currentProject.getPreferences(), currentProject.getRunconfigurations(), 
						currentProject.getLocation()));
			});
			removeItem.disableProperty().bind(row.emptyProperty());
						
			MenuItem showCounterExampleItem = new MenuItem("Show Counter Example");
			showCounterExampleItem.setOnAction(e-> showCounterExample());
			showCounterExampleItem.setDisable(true);
			
			row.setOnMouseClicked(e-> {
				if(e.getButton() == MouseButton.SECONDARY) {
					LTLFormulaItem item = tvFormula.getSelectionModel().getSelectedItem();
					if(row.emptyProperty().get() || item.getCounterExample() == null) {
						showCounterExampleItem.setDisable(true);
					} else {
						showCounterExampleItem.setDisable(false);
					}
				}
			});
			
			row.setContextMenu(new ContextMenu(removeItem, showCounterExampleItem));
			return row;
		});
		
		statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
		nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
		descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
		machineStatusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
		machineNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
		addLTLButton.disableProperty().bind(currentTrace.existsProperty().not());
		checkAllButton.disableProperty().bind(currentTrace.existsProperty().not());
		tvMachines.itemsProperty().bind(currentProject.machinesProperty());
		tvMachines.getFocusModel().focusedIndexProperty().addListener((observable, from, to) -> {
			if(to.intValue() >= 0) {
				tvFormula.itemsProperty().bind(tvMachines.getItems().get(to.intValue()).ltlFormulasProperty());
			}
		});
	}
		
	@FXML
	public void addFormula() {
		Machine machine = tvMachines.getFocusModel().getFocusedItem();
		injector.getInstance(LTLFormulaDialog.class).showAndWait().ifPresent(item -> {
			machine.addLTLFormula(item);
			currentProject.update(new Project(currentProject.getName(), currentProject.getDescription(), 
					currentProject.getMachines(), currentProject.getPreferences(), currentProject.getRunconfigurations(), 
					currentProject.getLocation()));
		});
	}
	
	public Checked checkFormula(LTLFormulaItem item) {
		LTL formula = null;
		LTLResultItem resultItem = null;
		Trace trace = null;
		try {
			formula = new LTL(item.getFormula());
			if (currentTrace != null) {
				State stateid = currentTrace.getCurrentState();
				EvaluationCommand lcc = formula.getCommand(stateid);
				currentTrace.getStateSpace().execute(lcc);
				AbstractEvalResult result = lcc.getValue();
				if(result instanceof LTLOk) {
					resultItem = new LTLResultItem(AlertType.INFORMATION, Checked.SUCCESS, "LTL Check succeeded", "Success");
				} else if(result instanceof LTLCounterExample) {
					trace = ((LTLCounterExample) result).getTrace(stateid.getStateSpace());
					resultItem = new LTLResultItem(AlertType.ERROR, Checked.FAIL, "LTL Counter Example has been found", 
													"Counter Example Found");
				} else if(result instanceof LTLError) {
					resultItem = new LTLResultItem(AlertType.ERROR, Checked.FAIL, ((LTLError) result).getMessage(), 
													"Error while executing formula");
				}
			}
		} catch (LtlParseException e) {
			StringWriter sw = new StringWriter();
			try (PrintWriter pw = new PrintWriter(sw)) {
				e.printStackTrace(pw);
			}
			resultItem = new LTLResultItem(AlertType.ERROR, Checked.FAIL, "Message: ", "Could not parse formula", 
											sw.toString(), true);
			logger.error("Could not parse LTL formula", e);
		}
		showResult(resultItem, item, trace);
		tvFormula.refresh();
		return resultItem.checked;
	}
	
	private void showResult(LTLResultItem resultItem, LTLFormulaItem item, Trace trace) {
		Alert alert = new Alert(resultItem.type, resultItem.message);
		alert.setTitle(item.getName());
		alert.setHeaderText(resultItem.header);
		if(resultItem.isParseError) {
			alert.getDialogPane().getStylesheets().add(getClass().getResource("/prob.css").toExternalForm());
			TextArea exceptionText = new TextArea(resultItem.exceptionText);
			exceptionText.setEditable(false);
			exceptionText.getStyleClass().add("text-area-error");
			StackPane pane = new StackPane(exceptionText);
			pane.setPrefSize(320, 120);
			alert.getDialogPane().setExpandableContent(pane);
			alert.getDialogPane().setExpanded(true);
		}
		alert.showAndWait();
		if(resultItem.type != AlertType.ERROR) {
			item.setCheckedSuccessful();
		} else {
			item.setCheckedFailed();
		}
		item.setCounterExample(trace);
	}
		
	private void showCurrentItemDialog(LTLFormulaItem item) {
		LTLFormulaDialog formulaDialog = injector.getInstance(LTLFormulaDialog.class);
		formulaDialog.setData(item.getName(), item.getDescription(), item.getFormula());
		formulaDialog.showAndWait().ifPresent(result-> {
			if(!item.getName().equals(result.getName()) || !item.getDescription().equals(result.getDescription()) || 
					!item.getFormula().equals(result.getFormula())) {
				item.setData(result.getName(), result.getDescription(), result.getFormula());
				tvFormula.refresh();
				currentProject.setSaved(false);
			}
		});
		formulaDialog.clear();
	}
	
	private void showCounterExample() {
		if (currentTrace.exists()) {
			this.animations.removeTrace(currentTrace.get());
		}
		animations.addNewAnimation(tvFormula.getSelectionModel().getSelectedItem().getCounterExample());
	}
	
	@FXML
	public void checkAll() {
		ArrayList<Boolean> success = new ArrayList<>();
		success.add(true);
		tvFormula.getItems().forEach(item-> {
			if(this.checkFormula(item) == Checked.FAIL) {
				tvMachines.getFocusModel().getFocusedItem().setCheckedFailed();
				success.set(0, false);
			};
		});
		if(success.get(0)) {
			tvMachines.getFocusModel().getFocusedItem().setCheckedSuccessful();
		}
		tvMachines.refresh();
	}

}
