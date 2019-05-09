package de.prob2.ui.verifications.ltl.formula;

import java.util.stream.Collectors;

import com.google.inject.Inject;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.verifications.AbstractResultHandler;
import de.prob2.ui.verifications.CheckingResultItem;
import de.prob2.ui.verifications.ltl.LTLCheckingResultItem;
import de.prob2.ui.verifications.ltl.LTLHandleItem;
import de.prob2.ui.verifications.ltl.LTLItemStage;
import de.prob2.ui.verifications.ltl.LTLMark;
import de.prob2.ui.verifications.ltl.LTLMarker;
import de.prob2.ui.verifications.ltl.LTLResultHandler;
import de.prob2.ui.verifications.ltl.LTLHandleItem.HandleType;
import javafx.fxml.FXML;
import netscape.javascript.JSObject;

public class LTLFormulaStage extends LTLItemStage<LTLFormulaItem> {
			
	@Inject
	public LTLFormulaStage(final StageManager stageManager, final CurrentProject currentProject, 
			final LTLFormulaChecker formulaChecker, final LTLResultHandler resultHandler) {
		super(currentProject, formulaChecker, resultHandler);
		stageManager.loadFXML(this, "ltlformula_stage.fxml"); 
	}
	
	@FXML
	private void applyFormula() {
		final JSObject editor = (JSObject) engine.executeScript("LtlEditor.cm");
		String code = editor.call("getValue").toString();
		if(handleItem.getHandleType() == HandleType.ADD) {
			addItem(currentProject.getCurrentMachine(), new LTLFormulaItem(code, taDescription.getText()));
		} else {
			changeItem(handleItem.getItem(), new LTLFormulaItem(code, taDescription.getText()));
		}
	}
	
	@Override
	protected void addItem(Machine machine, LTLFormulaItem item) {
		LTLFormulaChecker formulaChecker = (LTLFormulaChecker) ltlItemHandler;
		if(!machine.getLTLFormulas().contains(item)) {
			machine.addLTLFormula(item);
			updateProject();
			setHandleItem(new LTLHandleItem<LTLFormulaItem>(HandleType.CHANGE, item));
			formulaChecker.checkFormula(item, this);
			CheckingResultItem resultItem = item.getResultItem();
			if(resultItem != null) {
				taErrors.setText(resultItem.getMessage());
				markText(item);
			}
		} else {
			resultHandler.showAlreadyExists(AbstractResultHandler.ItemType.FORMULA);
		}
	}
	
	@Override
	protected void changeItem(LTLFormulaItem item, LTLFormulaItem result) {
		LTLFormulaChecker formulaChecker = (LTLFormulaChecker) ltlItemHandler;
		Machine machine = currentProject.getCurrentMachine();
		if(!machine.getLTLFormulas().stream()
				.filter(formula -> !formula.equals(item))
				.collect(Collectors.toList())
				.contains(result)) {
			item.setData(result.getName(), result.getDescription(), result.getCode());
			item.setCounterExample(null);
			item.setResultItem(null);
			currentProject.setSaved(false);
			setHandleItem(new LTLHandleItem<LTLFormulaItem>(HandleType.CHANGE, result));
			formulaChecker.checkFormula(item, this);
			CheckingResultItem resultItem = item.getResultItem();
			if(resultItem != null) {
				taErrors.setText(resultItem.getMessage());
				markText(item);
			}
		} else {
			resultHandler.showAlreadyExists(AbstractResultHandler.ItemType.FORMULA);
		}
	}
	
	public void setErrors(String text) {
		taErrors.setText(text);
	}

	private void markText(LTLFormulaItem item) {
		final JSObject editor = (JSObject) engine.executeScript("LtlEditor.cm");
		for(LTLMarker marker : ((LTLCheckingResultItem) item.getResultItem()).getErrorMarkers()) {
			LTLMark mark = marker.getMark();
			int line = mark.getLine() - 1;				
			JSObject from = (JSObject) engine.executeScript("from = {line:" + line +", ch:" + mark.getPos() +"}");
			JSObject to = (JSObject) engine.executeScript("to = {line:" + line +", ch:" + (mark.getPos() + mark.getLength()) +"}");
			JSObject style = (JSObject) engine.executeScript("style = {className:'error-underline'}");
			editor.call("markText", from, to, style);
		}
	}
}
