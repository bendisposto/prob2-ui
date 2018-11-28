package de.prob2.ui.formula;

import java.util.ResourceBundle;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.prob.animator.domainobjects.EvaluationException;
import de.prob.animator.domainobjects.IEvalElement;
import de.prob.exception.ProBError;
import de.prob2.ui.dynamic.DynamicCommandStatusBar;
import de.prob2.ui.helpsystem.HelpButton;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.layout.FontSize;
import de.prob2.ui.prob2fx.CurrentProject;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.MenuBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class FormulaStage extends Stage {
	private static final Logger logger = LoggerFactory.getLogger(FormulaStage.class);
	
	private static final String CSS_ERROR_CLASS = "text-field-error";

	@FXML
	private MenuBar menuBar;

	@FXML
	private TextField tfFormula;
	
	@FXML
	private TextArea taErrors;

	@FXML
	private ScrollPane formulaPane;
	
	@FXML
	private DynamicCommandStatusBar statusBar;
	
	@FXML
	private Button cancelButton;
	
	@FXML
	private HelpButton helpButton;
	
	private final StageManager stageManager;
	
	private final Injector injector;
	
	private final ResourceBundle bundle;
	
	private FormulaView formulaView;
	
	private final ObjectProperty<Thread> currentThread;
	
	private final CurrentProject currentProject;
	
	private final FontSize fontSize;
	
	private String currentFormula;

	@Inject
	public FormulaStage(final StageManager stageManager, final Injector injector, final ResourceBundle bundle, 
						final CurrentProject currentProject, final FontSize fontSize) {
		this.stageManager = stageManager;
		this.injector = injector;
		this.bundle = bundle;
		this.currentThread = new SimpleObjectProperty<>(this, "currentThread", null);
		this.currentProject = currentProject;
		this.fontSize = fontSize;
		stageManager.loadFXML(this, "formula_stage.fxml");
	}

	@FXML
	public void initialize() {
		stageManager.setMacMenuBar(this, menuBar);
		helpButton.setHelpContent(this.getClass());
		
		tfFormula.setOnKeyReleased(e -> {
			if (e.getCode() == KeyCode.ENTER) {
				apply();
			}
		});
		
		cancelButton.disableProperty().bind(currentThread.isNull());
		currentProject.currentMachineProperty().addListener((observable, from, to) -> {
			reset();
			tfFormula.clear();
		});
		fontSize.fontSizeProperty().addListener((observable, from, to) -> {
			if(currentFormula != null) {
				showFormula(currentFormula);
			}
		});
	}

	@FXML
	private void apply() {
		showFormula(tfFormula.getText());
	}
	
	public void showFormula(final Object formula) {
		FormulaGenerator formulaGenerator = injector.getInstance(FormulaGenerator.class);
		taErrors.clear();
		Thread thread = new Thread(() -> {
			try {
				Platform.runLater(() -> statusBar.setText(bundle.getString("statusbar.loadStatus.loading")));
				if(formula instanceof IEvalElement) {
					formulaView = formulaGenerator.showFormula((IEvalElement) formula);
					Platform.runLater(() -> {
						tfFormula.setText(((IEvalElement) formula).getCode());
						updateView(((IEvalElement)formula).getCode());
					});
				} else {
					formulaView = formulaGenerator.parseAndShowFormula((String) formula);
					Platform.runLater(() -> updateView((String) formula));
				}
			} catch (EvaluationException | ProBError exception) {
				taErrors.setText(exception.getMessage());
				handleError(exception);
			}
		});
		currentThread.set(thread);
		thread.start();
	}
	
	private void handleError(Exception exception) {
		logger.error("Evaluation of formula failed", exception);
		Platform.runLater(() -> {
			reset();
			if(!tfFormula.getStyleClass().contains(CSS_ERROR_CLASS)) {
				tfFormula.getStyleClass().add(CSS_ERROR_CLASS);
			}
		});
	}
	
	private void updateView(String formula) {
		Platform.runLater(() -> {
			formulaPane.setContent(formulaView);
			tfFormula.getStyleClass().remove(CSS_ERROR_CLASS);
			statusBar.setText("");
			currentThread.set(null);
			currentFormula = formula;
		});
	}
	
	@FXML
	private void cancel() {
		if (currentThread.get() != null) {
			currentThread.get().interrupt();
			currentThread.set(null);
		}
		reset();
	}
	
	private void reset() {
		if(formulaView != null) {
			formulaView.getChildren().clear();
		}
		statusBar.setText("");
		currentThread.set(null);
	}
	
	@FXML
	private void zoomIn() {
		zoom(1.3);
	}
	
	@FXML
	private void zoomOut() {
		zoom(0.8);
	}
	
	@FXML
	private void defaultSize() {
		if(formulaView == null) {
			return;
		}
		formulaView.defaultSize();
	}
	
	private void zoom(double factor) {
		if(formulaView == null) {
			return;
		}
		formulaView.zoomByFactor(factor);
		formulaPane.setHvalue(formulaPane.getHvalue() * factor);
		formulaPane.setVvalue(formulaPane.getVvalue() * factor);
	}
	
	@FXML
	private void handleClose() {
		currentThread.set(null);
		this.close();
	}
	
}
