package de.prob2.ui.modelchecking;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.eventbus.EventBus;
import com.google.inject.Inject;

import de.prob.animator.command.ComputeCoverageCommand.ComputeCoverageResult;
import de.prob.check.IModelCheckListener;
import de.prob.check.IModelCheckingResult;
import de.prob.check.LTLOk;
import de.prob.check.ModelCheckOk;
import de.prob.check.ModelChecker;
import de.prob.check.StateSpaceStats;
import de.prob.statespace.ITraceDescription;
import de.prob2.ui.events.ModelCheckStatsEvent;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;

public class ModelCheckStats extends AnchorPane implements IModelCheckListener {
	@FXML
	private Label elapsedTime;
	@FXML
	private Label processedNodes;
	@FXML
	private Label totalNodes;
	@FXML
	private Label totalTransitions;
	@FXML
	private GridPane nodeStats;
	@FXML
	private GridPane transStats;

	private Map<String, ModelChecker> jobs = new HashMap<String, ModelChecker>();
	Map<String, IModelCheckingResult> results = new HashMap<String, IModelCheckingResult>();

	private EventBus bus;

	@Inject
	public ModelCheckStats(FXMLLoader loader, EventBus bus) {
		this.bus = bus;
		try {
			loader.setLocation(getClass().getResource("modelchecking_stats.fxml"));
			loader.setRoot(this);
			loader.setController(this);
			loader.load();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void updateStats(final String id, final long timeElapsed, final IModelCheckingResult result,
			final StateSpaceStats stats) {
		results.put(id, result);
		Platform.runLater(() -> {
			elapsedTime.setText("" + timeElapsed);
		});
		boolean hasStats = stats != null;

		if (hasStats) {
			int nrProcessedNodes = stats.getNrProcessedNodes();
			int nrTotalNodes = stats.getNrTotalNodes();
			int nrTotalTransitions = stats.getNrTotalTransitions();
			int percent = nrProcessedNodes * 100 / nrTotalNodes;
			Platform.runLater(() -> {
				processedNodes.setText("" + nrProcessedNodes + " (" + percent + " %)");
				totalNodes.setText("" + nrTotalNodes);
				totalTransitions.setText("" + nrTotalTransitions);
			});
		}
		System.out.println("updated Stats");
	}

	@Override
	public void isFinished(final String id, final long timeElapsed, final IModelCheckingResult result,
			final StateSpaceStats stats) {
		results.put(id, result);
		String message = result.getMessage();

		Platform.runLater(() -> {
			elapsedTime.setText("" + timeElapsed);
		});

		String res = result instanceof ModelCheckOk || result instanceof LTLOk ? "success"
				: result instanceof ITraceDescription ? "danger" : "warning";
		boolean hasTrace = result instanceof ITraceDescription;
		ModelChecker modelChecker = jobs.get(id);
		ComputeCoverageResult coverage = null;

		if (modelChecker != null) {
			coverage = modelChecker.getCoverage();
		}

		jobs.remove(id);
		if (coverage != null) {
			Number numNodes = coverage.getTotalNumberOfNodes();
			Number numTrans = coverage.getTotalNumberOfTransitions();

			Platform.runLater(() -> {
				totalNodes.setText("" + numNodes);
				totalTransitions.setText("" + numTrans);
			});

			showStats(coverage.getNodes(), nodeStats);
			showStats(coverage.getOps(), transStats);

			// List<String> uncovered = coverage.getUncovered();
			// for (String transition : uncovered) {
			// transStats.add(WebUtils.wrap("name", transition, "value", "0"));
			// }
			// String transitionStats = WebUtils.toJson(transStats);
		}
		bus.post(new ModelCheckStatsEvent(this, res, message));
		System.out.println("is finished");
	}

	private void showStats(List<String> packedStats, GridPane grid) {
		Platform.runLater(() -> {
			grid.getChildren().clear();
		});
		for (String pStat : packedStats) {
			String woPre = pStat.startsWith("'") ? pStat.substring(1) : pStat;
			String woSuf = woPre.endsWith("'") ? woPre.substring(0, woPre.length() - 1) : woPre;
			String[] split = woSuf.split(":");
			Stat stat = null;
			if (split.length == 2) {
				stat = new Stat(split[0], split[1]);
			} else if (split.length == 1) {
				stat = new Stat(split[0], null);
			}
			Node[] statFX = stat.toFX();
			Platform.runLater(() -> {
				grid.addRow(packedStats.indexOf(pStat) + 1, statFX);
			});
		}
	}

	void addJob(String jobId, ModelChecker checker) {
		jobs.put(jobId, checker);
	}
}
