package de.prob2.ui.verifications.modelchecking;

import de.prob.animator.command.ComputeCoverageCommand;
import de.prob.check.*;
import de.prob.statespace.ITraceDescription;
import de.prob.statespace.StateSpace;
import de.prob.statespace.Trace;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.stats.StatsView;
import de.prob2.ui.statusbar.StatusBar;
import de.prob2.ui.verifications.Checked;
import de.prob2.ui.verifications.MachineTableView;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;

import javax.inject.Inject;

import com.google.inject.Injector;

import java.util.Objects;


public final class ModelCheckStats extends AnchorPane {
	
	@FXML private AnchorPane resultBackground;
	@FXML private Text resultText;
	@FXML private VBox statsBox;
	@FXML private Label elapsedTime;
	@FXML private Label processedStates;
	@FXML private Label totalStates;
	@FXML private Label totalTransitions;

	private ModelcheckingController modelcheckingController;
	private Trace trace;
	
	private final StatsView statsView;
	
	private ModelCheckingItem item;
	
	private final Injector injector;
	
	private Machine currentMachine;
	
	@Inject
	public ModelCheckStats(final StageManager stageManager, final ModelcheckingController modelcheckingController, final StatsView statsView,
							final Injector injector) {
		this.modelcheckingController = modelcheckingController;
		this.statsView = statsView;
		this.injector = injector;
		stageManager.loadFXML(this, "modelchecking_stats.fxml");
	}

	@FXML
	private void initialize() {
		this.modelcheckingController.widthProperty().addListener((observableValue, oldValue, newValue) -> {
			if (newValue == null) {
				resultText.setWrappingWidth(0);
				return;
			}
			resultText.setWrappingWidth(newValue.doubleValue() - 60);
		});
	}

	void startJob() {
		statsBox.setVisible(true);
		resultBackground.setVisible(false);
	}

	public void updateStats(final IModelCheckJob modelChecker, final long timeElapsed, final StateSpaceStats stats) {
		Objects.requireNonNull(modelChecker, "modelChecker");
		
		Platform.runLater(() -> elapsedTime.setText(String.valueOf(timeElapsed)));

		if (stats != null) {
			int nrProcessedNodes = stats.getNrProcessedNodes();
			int nrTotalNodes = stats.getNrTotalNodes();
			int nrTotalTransitions = stats.getNrTotalTransitions();
			int percent = nrProcessedNodes * 100 / nrTotalNodes;
			Platform.runLater(() -> {
				processedStates.setText(nrProcessedNodes + " (" + percent + " %)");
				totalStates.setText(String.valueOf(nrTotalNodes));
				totalTransitions.setText(String.valueOf(nrTotalTransitions));
			});
		}
		
		final StateSpace stateSpace = modelChecker.getStateSpace();
		final ComputeCoverageCommand cmd = new ComputeCoverageCommand();
		stateSpace.execute(cmd);
		if (cmd.isInterrupted()) {
			Thread.currentThread().interrupt();
			return;
		}
		final ComputeCoverageCommand.ComputeCoverageResult coverage = cmd.getResult();
		
		if (coverage != null) {
			statsView.updateExtendedStats(coverage);
		}
	}

	public void isFinished(final IModelCheckJob modelChecker, final long timeElapsed, final IModelCheckingResult result) {
		Objects.requireNonNull(modelChecker, "modelChecker");
		Objects.requireNonNull(result, "result");
		
		Platform.runLater(() -> elapsedTime.setText(String.valueOf(timeElapsed)));
		
		if (result instanceof ModelCheckOk || result instanceof LTLOk) {
			item.setCheckedSuccessful();
			item.setChecked(Checked.SUCCESS);
		} else if (result instanceof ITraceDescription) {
			item.setCheckedFailed();
			item.setChecked(Checked.FAIL);
		} else {
			item.setTimeout();
			item.setChecked(Checked.TIMEOUT);
		}
		item.setStats(this);
		updateCurrentMachineStatus();
		String message = result.getMessage();

		final StateSpace stateSpace = modelChecker.getStateSpace();
		final ComputeCoverageCommand cmd = new ComputeCoverageCommand();
		stateSpace.execute(cmd);
		final ComputeCoverageCommand.ComputeCoverageResult coverage = cmd.getResult();
		
		if (coverage != null) {
			statsView.updateExtendedStats(coverage);
			Number numNodes = coverage.getTotalNumberOfNodes();
			Number numTrans = coverage.getTotalNumberOfTransitions();

			Platform.runLater(() -> {
				totalStates.setText(String.valueOf(numNodes));
				totalTransitions.setText(String.valueOf(numTrans));
			});
		}
		
		if (result instanceof ITraceDescription) {
			StateSpace s = modelChecker.getStateSpace();
			trace = ((ITraceDescription) result).getTrace(s);
		}
		showResult(message);
	}
	
	private void updateCurrentMachineStatus() {
		for(ModelCheckingItem item : currentMachine.getModelcheckingItems()) {
			if(item.getChecked() == Checked.FAIL) {
				currentMachine.setModelcheckingCheckedFailed();
				injector.getInstance(MachineTableView.class).refresh();
				injector.getInstance(StatusBar.class).setModelcheckingStatus(StatusBar.ModelcheckingStatus.ERROR);
				return;
			}
		}
		currentMachine.setModelcheckingCheckedSuccessful();
		injector.getInstance(MachineTableView.class).refresh();
		injector.getInstance(StatusBar.class).setModelcheckingStatus(StatusBar.ModelcheckingStatus.SUCCESSFUL);
	}

	private void showResult(String message) {
		resultBackground.setVisible(true);
		resultText.setText(message);
		resultText.setWrappingWidth(this.modelcheckingController.widthProperty().doubleValue() - 60);
		switch (item.getChecked()) {
			case SUCCESS:
				resultBackground.getStyleClass().setAll("mcheckSuccess");
				resultText.setFill(Color.web("#5e945e"));
				break;

			case FAIL:
				resultBackground.getStyleClass().setAll("mcheckDanger");
				resultText.setFill(Color.web("#b95050ff"));
				break;

			case TIMEOUT:
				resultBackground.getStyleClass().setAll("mcheckWarning");
				resultText.setFill(Color.web("#96904e"));
				break;

			default:
				throw new IllegalArgumentException("Unknown result: " + item.getChecked());
		}
	}
	
	public Trace getTrace() {
		return trace;
	}
	
	public void setBackgroundOnClick(EventHandler<? super MouseEvent> eventHandler) {
		resultBackground.setOnMouseClicked(eventHandler);
	}
	
	public void updateItem(ModelCheckingItem item, Machine currentMachine) {
		this.item = item;
		this.currentMachine = currentMachine;
	}
	
}
