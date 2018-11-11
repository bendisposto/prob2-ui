package de.prob2.ui.menu;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.prob2.ui.chart.HistoryChartStage;
import de.prob2.ui.dynamic.dotty.DotView;
import de.prob2.ui.dynamic.table.ExpressionTableView;
import de.prob2.ui.formula.FormulaStage;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.visualisation.magiclayout.MagicLayoutView;
import javafx.fxml.FXML;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.stage.Stage;

/**
 * @author Christoph Heinzen
 * @since 21.09.17
 */
@Singleton
public class VisualisationMenu extends Menu {
	@FXML private MenuItem enterFormulaForVisualization;
	@FXML private MenuItem graphVisualization;
	@FXML private MenuItem tableVisualization;
	
	private final Injector injector;
	private final CurrentTrace currentTrace;
	private final CurrentProject currentProject;

	@Inject
	public VisualisationMenu(final StageManager stageManager, final Injector injector, final CurrentTrace currentTrace, final CurrentProject currentProject) {
		this.currentTrace = currentTrace;
		this.currentProject = currentProject;
		this.injector = injector;
		stageManager.loadFXML(this, "visualisationMenu.fxml");
	}

	@FXML
	public void initialize() {
		this.enterFormulaForVisualization.disableProperty()
				.bind(currentTrace.currentStateProperty().initializedProperty().not());
		this.graphVisualization.disableProperty().bind(currentProject.currentMachineProperty().isNull());
		this.tableVisualization.disableProperty().bind(currentProject.currentMachineProperty().isNull());
	}
	
	@FXML
	private void openGraphVisualisation() {
		injector.getInstance(DotView.class).show();
	}
	
	@FXML
	private void openTableVisualisation() {
		injector.getInstance(ExpressionTableView.class).show();
	}
	
	@FXML
	private void openMagicLayout() {
		MagicLayoutView magicLayout = injector.getInstance(MagicLayoutView.class);
		magicLayout.show();
		magicLayout.toFront();
	}

	@FXML
	private void handleFormulaInput() {
		injector.getInstance(FormulaStage.class).show();
	}

	@FXML
	private void handleHistoryChart() {
		final Stage chartStage = injector.getInstance(HistoryChartStage.class);
		chartStage.show();
		chartStage.toFront();
	}

}
