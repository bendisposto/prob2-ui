package de.prob2.ui.verifications.ltl;


import com.google.inject.Inject;
import com.google.inject.Injector;

import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

public class LTLFormulaStage extends Stage {
	
	@FXML
	private TextArea taFormula;
	
	@FXML
	private Button checkFormulaButton;
	
	private final CurrentTrace currentTrace;
	
	private final CurrentProject currentProject;
	
	private LTLFormulaItem item;
	
	private Injector injector;
		
	@Inject
	private LTLFormulaStage(final StageManager stageManager, final Injector injector, final CurrentTrace currentTrace,
							final CurrentProject currentProject) {
		this.injector = injector;
		this.currentTrace = currentTrace;
		this.currentProject = currentProject;
		this.item = null;
		stageManager.loadFXML(this, "ltlformula_stage.fxml");
	}
	
	@FXML
	public void initialize() {
		taFormula.textProperty().addListener((observable, from, to) -> {
			if(from != null) {
				currentProject.setSaved(false);
			}
		});
		checkFormulaButton.disableProperty().bind(currentTrace.existsProperty().not());
	}
	
	public void setItem(LTLFormulaItem item) {
		this.item = item;
		taFormula.setText(item.getFormula());
	}
	
	@FXML
	private void handleSave() {
		
	}
	
	@FXML
	private void handleClose() {
		this.close();
	}
	
	@FXML
	public void checkFormula() {
		injector.getInstance(LTLView.class).checkFormula(item);
	}
	

}
