package de.prob2.ui.modelchecking;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.google.inject.Inject;

import de.prob.animator.command.ComputeCoverageCommand.ComputeCoverageResult;
import de.prob.check.IModelCheckListener;
import de.prob.check.IModelCheckingResult;
import de.prob.check.LTLOk;
import de.prob.check.ModelCheckOk;
import de.prob.check.ModelChecker;
import de.prob.check.StateSpaceStats;
import de.prob.statespace.ITraceDescription;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;

public class ModelCheckStats extends AnchorPane implements IModelCheckListener {
	@FXML
	private Label elapsedTime;

	@FXML
	private Label processedNodes;

	@FXML
	private Label totalNodes;

	@FXML
	private Label totalTransitions;

	private Map<String, ModelChecker> jobs = new HashMap<String, ModelChecker>();

	private ModelCheckStatsView mCheckView;

	@Inject
	public ModelCheckStats(FXMLLoader loader, ModelCheckStatsView mCheckView) {
		this.mCheckView = mCheckView;
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
		// results.put(id, result);
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
				processedNodes.setText("" + nrProcessedNodes);
				totalNodes.setText("" + nrTotalNodes);
				totalTransitions.setText("" + nrTotalTransitions);
			});

			// submit(WebUtils.wrap("cmd", "ModelChecking.updateJob", "id", id,
			// "stats", hasStats, "processedNodes", nrProcessedNodes,
			// "totalNodes", nrTotalNodes, "totalTransitions",
			// stats.getNrTotalTransitions(), "percent", percent, "time",
			// timeElapsed));
		}
		// else {
		// submit(WebUtils.wrap("cmd", "ModelChecking.updateJob", "id", id,
		// "stats", hasStats, "percent", 100, "time", timeElapsed));
		// }
		System.out.println("updated Stats");
	}

	@Override
	public void isFinished(final String id, final long timeElapsed, final IModelCheckingResult result,
			final StateSpaceStats stats) {
		// results.put(id, result);

		String res = result instanceof ModelCheckOk || result instanceof LTLOk ? "success"
				: result instanceof ITraceDescription ? "danger" : "warning";
		System.out.println(res);
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
			//
			// String nodeStats = WebUtils.toJson(extractNodeStats(coverage
			// .getNodes()));
			// List<Map<String, String>> transStats = extractNodeStats(coverage
			// .getOps());
			// List<String> uncovered = coverage.getUncovered();
			// for (String transition : uncovered) {
			// transStats.add(WebUtils.wrap("name", transition, "value", "0"));
			// }
			// String transitionStats = WebUtils.toJson(transStats);
			// submit(WebUtils.wrap("cmd", "ModelChecking.finishJob", "id", id,
			// "time", timeElapsed, "stats", true, "processedNodes",
			// numNodes, "totalNodes", numNodes, "totalTransitions",
			// numTrans, "result", res, "hasTrace", hasTrace, "message",
			// result.getMessage(), "nodeStats", nodeStats, "transStats",
			// transitionStats));
		}
		// else {
		// Map<String, String> wrap = WebUtils.wrap("cmd",
		// "ModelChecking.finishJob", "id", id, "time", timeElapsed,
		// "stats", false, "result", res, "hasTrace", hasTrace,
		// "message", result.getMessage());
		// submit(wrap);
		// }
		mCheckView.showStats(this, res);
		System.out.println("is finished");
	}

	void addJob(String jobId, ModelChecker checker) {
		jobs.put(jobId, checker);
	}
}
