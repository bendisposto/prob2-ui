package de.prob2.ui.verifications.modelchecking;

import java.util.ResourceBundle;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.prob.check.ModelCheckingOptions;
import de.prob.check.ModelCheckingOptions.Options;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentTrace;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.StringConverter;

@FXMLInjected
@Singleton
public class ModelcheckingStage extends Stage {
	
	public enum SearchStrategy {
		MIXED_BF_DF("verifications.modelchecking.modelcheckingStage.strategy.mixedBfDf"),
		BREADTH_FIRST("verifications.modelchecking.modelcheckingStage.strategy.breadthFirst"),
		DEPTH_FIRST("verifications.modelchecking.modelcheckingStage.strategy.depthFirst"),
		//HEURISTIC_FUNCTION("verifications.modelchecking.modelcheckingStage.strategy.heuristicFunction"),
		//HASH_RANDOM("verifications.modelchecking.modelcheckingStage.strategy.hashRandom"),
		//RANDOM("verifications.modelchecking.modelcheckingStage.strategy.random"),
		//OUT_DEGREE("verifications.modelchecking.modelcheckingStage.strategy.outDegree"),
		//DISABLED_TRANSITIONS("verifications.modelchecking.modelcheckingStage.strategy.disabledTransitions"),
		;
		
		private final String name;
		
		SearchStrategy(final String name) {
			this.name = name;
		}
		
		public String getName() {
			return this.name;
		}
	}
	
	@FXML
	private Button startButton;
	@FXML
	private ChoiceBox<SearchStrategy> selectSearchStrategy;
	@FXML
	private CheckBox findDeadlocks;
	@FXML
	private CheckBox findInvViolations;
	@FXML
	private CheckBox findBAViolations;
	@FXML
	private CheckBox findGoal;
	@FXML
	private CheckBox stopAtFullCoverage;
	
	private final ResourceBundle bundle;
	
	private final StageManager stageManager;
	
	private final CurrentTrace currentTrace;
	
	private final Injector injector;

	@Inject
	private ModelcheckingStage(final StageManager stageManager, final ResourceBundle bundle, 
							final CurrentTrace currentTrace, final Injector injector) {
		this.bundle = bundle;
		this.stageManager = stageManager;
		this.currentTrace = currentTrace;
		this.injector = injector;
		stageManager.loadFXML(this, "modelchecking_stage.fxml");
	}

	@FXML
	private void initialize() {
		this.initModality(Modality.APPLICATION_MODAL);
		this.selectSearchStrategy.getItems().setAll(SearchStrategy.values());
		this.selectSearchStrategy.setValue(SearchStrategy.MIXED_BF_DF);
		this.selectSearchStrategy.setConverter(new StringConverter<SearchStrategy>() {
			@Override
			public String toString(final SearchStrategy object) {
				return bundle.getString(object.getName());
			}
			
			@Override
			public SearchStrategy fromString(final String string) {
				throw new UnsupportedOperationException("Conversion from String to SearchStrategy not supported");
			}
		});
	}

	@FXML
	private void startModelCheck() {
		if (currentTrace.exists()) {
			injector.getInstance(Modelchecker.class).checkItem(getOptions(), selectSearchStrategy.getConverter(), selectSearchStrategy.getValue());
		} else {
			stageManager.makeAlert(Alert.AlertType.ERROR, "",
					"verifications.modelchecking.modelcheckingStage.alerts.noMachineLoaded.content")
					.showAndWait();
			this.hide();
		}
	}
	

	
	private ModelCheckingOptions getOptions() {
		ModelCheckingOptions options = new ModelCheckingOptions();
		
		switch (selectSearchStrategy.getValue()) {
			case MIXED_BF_DF:
				break;
			case BREADTH_FIRST:
				options = options.breadthFirst(true);
				break;
			case DEPTH_FIRST:
				options = options.depthFirst(true);
				break;
			default:
				throw new IllegalArgumentException("Unhandled search strategy: " + selectSearchStrategy.getValue());
		}
		
		options = options.checkDeadlocks(findDeadlocks.isSelected());
		options = options.checkInvariantViolations(findInvViolations.isSelected());
		options = options.checkAssertions(findBAViolations.isSelected());
		options = options.checkGoal(findGoal.isSelected());
		options = options.stopAtFullCoverage(stopAtFullCoverage.isSelected());
		options = options.recheckExisting(true); // TO DO: set to false if we want new problems; currently seems to have no influence on prob2_interface calls; currently the first call always contains inspect_existing_nodes as an option, e.g., do_modelchecking(500,[find_deadlocks,find_invariant_violations,inspect_existing_nodes],Result,Stats)
		return options;
	}

	@FXML
	private void cancel() {
		injector.getInstance(ModelcheckingView.class).cancelModelcheck();
		this.hide();
	}

	public void setDisableStart(final boolean disableStart) {
		Platform.runLater(() -> this.startButton.setDisable(disableStart));
	}
	
	public void show(ModelCheckingOptions options) {
		reset();
		setDisableModelcheck(true);
		if(!options.getPrologOptions().contains(Options.BREADTH_FIRST_SEARCH) && 
				!options.getPrologOptions().contains(Options.DEPTH_FIRST_SEARCH)) {
			selectSearchStrategy.getSelectionModel().select(SearchStrategy.MIXED_BF_DF);
		}
		for(Options option : options.getPrologOptions()) {
			switch(option) {
				case BREADTH_FIRST_SEARCH:
					selectSearchStrategy.getSelectionModel().select(SearchStrategy.BREADTH_FIRST);
					break;
				case DEPTH_FIRST_SEARCH:
					selectSearchStrategy.getSelectionModel().select(SearchStrategy.DEPTH_FIRST);
					break;
				case FIND_DEADLOCKS:
					findDeadlocks.setSelected(true);
					break;
				case FIND_INVARIANT_VIOLATIONS:
					findInvViolations.setSelected(true);
					break;
				case FIND_ASSERTION_VIOLATIONS:
					findBAViolations.setSelected(true);
					break;
				case INSPECT_EXISTING_NODES:
					break;
				case STOP_AT_FULL_COVERAGE:
					stopAtFullCoverage.setSelected(true);
					break;
				case PARTIAL_ORDER_REDUCTION:
					break;
				case PARTIAL_GUARD_EVALUATION:
					break;
				case FIND_GOAL:
					findGoal.setSelected(true);
					break;
				default:
					break;
			}
		}
		this.show();
	}
	
	private void reset() {
		findDeadlocks.setSelected(false);
		findInvViolations.setSelected(false);
		findBAViolations.setSelected(false);
		findGoal.setSelected(false);
		stopAtFullCoverage.setSelected(false);
	}
	
	public void setDisableModelcheck(boolean disable) {
		startButton.setDisable(disable);
		selectSearchStrategy.setDisable(disable);
		findDeadlocks.setDisable(disable);
		findInvViolations.setDisable(disable);
		findBAViolations.setDisable(disable);
		findGoal.setDisable(disable);
		stopAtFullCoverage.setDisable(disable);
	}
}
