package de.prob2.ui.dotty;


import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.ResourceBundle;

import com.google.inject.Inject;

import de.prob.Main;
import de.prob.animator.command.GetAllDotCommands;
import de.prob.animator.command.GetSvgForVisualizationCommand;
import de.prob.animator.domainobjects.ClassicalB;
import de.prob.animator.domainobjects.DotCommandItem;
import de.prob.animator.domainobjects.EvaluationException;
import de.prob.animator.domainobjects.IEvalElement;
import de.prob.exception.ProBError;
import de.prob.statespace.State;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentTrace;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DotView extends Stage {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(DotView.class);
	
	private static final File FILE = new File(Main.getProBDirectory()
			+ File.separator + "prob2ui" + File.separator + "out.svg");
	
	@FXML
	private WebView dotView;
	
	@FXML
	private ChoiceBox<DotCommandItem> cbChoice;
	
	@FXML
	private TextField tfFormula;
	
	@FXML
	private HBox enterFormulaBox;
	
	@FXML
	private Label lbDescription;
	
	@FXML
	private Label lbAvailable;
	
	@FXML
	private Button visualizeButton;
	
	@FXML
	private ScrollPane pane;
	
	private double oldMousePositionX = -1;
	private double oldMousePositionY = -1;
	private double dragFactor = 0.83;
	
	private final StageManager stageManager;
	private final CurrentTrace currentTrace;
	private final ResourceBundle bundle;
	
	
	@Inject
	public DotView(final StageManager stageManager, final CurrentTrace currentTrace, final ResourceBundle bundle) {
		this.stageManager = stageManager;
		this.currentTrace = currentTrace;
		this.bundle = bundle;
		stageManager.loadFXML(this, "dot_view.fxml");
	}
	
	@FXML
	public void initialize() {
		dotView.setContextMenuEnabled(false);
		dotView.setOnMouseMoved(e-> {
			oldMousePositionX = e.getSceneX();
			oldMousePositionY = e.getSceneY();
		});
		
		dotView.setOnMouseDragged(e-> {
			pane.setHvalue(pane.getHvalue() + (-e.getSceneX() + oldMousePositionX)/(pane.getWidth() * dragFactor));
			pane.setVvalue(pane.getVvalue() + (-e.getSceneY() + oldMousePositionY)/(pane.getHeight() * dragFactor));
			oldMousePositionX = e.getSceneX();
			oldMousePositionY = e.getSceneY();
		});

		dotView.setOnMouseClicked(e-> {
			if(e.getClickCount() < 2) {
				return;
			}
			
			if(e.getButton() == MouseButton.SECONDARY) {
				dotView.setZoom(dotView.getZoom() * 0.9);
				dragFactor *= 0.9;
			} else {
				dotView.setZoom(dotView.getZoom() * 1.1);
				dragFactor *= 1.1;
			}
			
			double x = e.getX()/(2*dragFactor);
			double y = e.getY()/(2*dragFactor);
			dotView.getEngine().executeScript("window.scrollBy(" + x + "," + y +")");
		});
		
		cbChoice.getSelectionModel().selectFirst();
		cbChoice.getSelectionModel().selectedItemProperty().addListener((observable, from, to) -> {
			dotView.getEngine().loadContent("");
			if(to == null) {
				return;
			}
			if(!to.isAvailable()) {
				visualizeButton.setDisable(true);
				lbAvailable.setText(bundle.getString("dotview.notavailable"));
			} else {
				visualizeButton.setDisable(false);
				lbAvailable.setText("");
			}
			enterFormulaBox.setVisible(to.getArity() > 0);
			lbDescription.setText(to.getDescription());
		});
		fillCommands();
		currentTrace.currentStateProperty().addListener((observable, from, to) ->  {
			int index = cbChoice.getSelectionModel().getSelectedIndex();
			fillCommands();
			if(index == -1) {
				return;
			}
			cbChoice.getSelectionModel().select(index);
		});
	}
	
	private void fillCommands() {
		try {
			cbChoice.getItems().clear();
			State id = currentTrace.getCurrentState();
			GetAllDotCommands cmd = new GetAllDotCommands(id);
			currentTrace.getStateSpace().execute(cmd);
			for(DotCommandItem item : cmd.getCommands()) {
				cbChoice.getItems().add(item);
			}
		} catch (Exception e) {
			LOGGER.error("Extract all dot commands failed", e);
		}
	}
	
	@FXML
	public void visualize() {
		ArrayList<IEvalElement> formulas = new ArrayList<>();
		try {
			DotCommandItem item = cbChoice.getValue();
			if(item != null) {
				if(item.getArity() > 0) {
					formulas.add(new ClassicalB(tfFormula.getText()));
				}
				State id = currentTrace.getCurrentState();
				GetSvgForVisualizationCommand cmd = new GetSvgForVisualizationCommand(id, item, FILE, formulas);
				currentTrace.getStateSpace().execute(cmd);
				loadGraph();
			}
		} catch (IOException | ProBError | EvaluationException e) {
			LOGGER.error("Graph visualization failed", e);
			stageManager.makeExceptionAlert(bundle.getString("dotview.error.message"), e).show();
		}
	}
	
	private void loadGraph() throws IOException {
		String content = new String(Files.readAllBytes(FILE.toPath()));
		dotView.getEngine().loadContent("<center>" + content + "</center>");
	}

}
