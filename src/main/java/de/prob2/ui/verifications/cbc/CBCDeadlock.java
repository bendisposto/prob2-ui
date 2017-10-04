package de.prob2.ui.verifications.cbc;

import java.util.Arrays;
import java.util.ResourceBundle;

import javax.inject.Inject;

import com.google.inject.Injector;

import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.verifications.cbc.CBCFormulaItem.CBCType;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Modality;

public class CBCDeadlock extends AbstractCBCFormulaInputStage {
	
	
	@FXML
	private TextField tfFormula;
	
	@FXML
	private Button findDeadlockButton;
	
	@FXML
	private Button findValidStateButton;
	
	@Inject
	private CBCDeadlock(final StageManager stageManager, final CBCFormulaHandler cbcHandler,
						final CurrentProject currentProject, final Injector injector, final ResourceBundle bundle) {
		super(cbcHandler, currentProject, injector, bundle);
		stageManager.loadFXML(this, "cbc_deadlock.fxml");
		this.initModality(Modality.APPLICATION_MODAL);
	}
	
	@FXML
	public void initialize() {
		this.invisibles.addAll(Arrays.asList(findDeadlockButton, findValidStateButton));
	}
			
	@FXML
	public void addFormula() {
		addFormula(false);
	}
	
	private void addFormula(boolean checking) {
		cbcHandler.addFormula(tfFormula.getText(), tfFormula.getText(), CBCFormulaItem.CBCType.DEADLOCK,
								checking);
		this.close();
	}
	
	@FXML
	public void checkFormula() {
		addFormula(true);
		cbcHandler.checkDeadlock(tfFormula.getText());
		this.close();
	}
	
	@FXML
	public void findDeadlock() {
		cbcHandler.addFormula("FIND DEADLOCK", "FIND DEADLOCK", CBCFormulaItem.CBCType.FIND_DEADLOCK, true);
		cbcHandler.findDeadlock();
		this.close();
	}
	
	@FXML
	public void findValidState() {
		CBCFormulaItem item = new CBCFormulaItem(tfFormula.getText(), tfFormula.getText(), 
				CBCType.FIND_VALID_STATE);
		cbcHandler.addFormula(item, true);
		cbcHandler.findValidState(item);
		this.close();
	}
	
	public void changeFormula(CBCFormulaItem item) {
		super.changeFormula(tfFormula, item, invisibles);
	}
	
	
	@FXML
	public void cancel() {
		this.close();
	}
		
}
