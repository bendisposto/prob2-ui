package de.prob2.ui.verifications.ltl;

import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.project.Project;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.verifications.ltl.patterns.builtins.LTLBuiltinsStage;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;

import netscape.javascript.JSObject;

public abstract class LTLItemStage<T extends ILTLItem> extends Stage {
	
	@FXML
	protected WebView taCode;
	
	@FXML
	protected TextArea taDescription;
	
	@FXML
	protected TextArea taErrors;
	
	protected final CurrentProject currentProject;
	
	protected final ILTLItemHandler ltlItemHandler;
	
	protected final LTLResultHandler resultHandler;
	
	protected final LTLBuiltinsStage builtinsStage;
	
	protected LTLHandleItem<T> handleItem;
	
	protected WebEngine engine;

	public LTLItemStage(final CurrentProject currentProject, final ILTLItemHandler ltlItemHandler, final LTLResultHandler resultHandler, final LTLBuiltinsStage builtinsStage) {
		super();
		this.currentProject = currentProject;
		this.ltlItemHandler = ltlItemHandler;
		this.resultHandler = resultHandler;
		this.builtinsStage = builtinsStage;
		this.initModality(Modality.APPLICATION_MODAL);
	}
	
	@FXML
	public void initialize() {
		engine = taCode.getEngine();
		engine.load(LTLItemStage.class.getResource("LTLEditor.html").toExternalForm());
		engine.setJavaScriptEnabled(true);
	}
	
	@FXML
	protected void showBuiltins() {
		builtinsStage.show();
	}
	
	private void setTextEditor(String text) {
		final JSObject editor = (JSObject) engine.executeScript("LtlEditor.cm");
		editor.call("setValue", text);
	}
		
	public void setData(String description, String code) {
		taDescription.setText(description);
		setTextEditor(code);
	}
	
	public void clear() {
		this.taDescription.clear();
	}

	public WebEngine getEngine() {
		return engine;
	}
	
	protected void updateProject() {
		currentProject.update(new Project(currentProject.getName(), currentProject.getDescription(), 
				currentProject.getMachines(), currentProject.getPreferences(), currentProject.getLocation()));
	}
	
	public void setHandleItem(LTLHandleItem<T> handleItem) {
		this.handleItem = handleItem;
	}
	
	protected abstract void addItem(Machine machine, T item);
	
	protected abstract void changeItem(T item, T result);
	
	protected void showErrors(LTLCheckingResultItem resultItem) {
		if(resultItem == null) {
			this.close();
			return;
		}
		taErrors.setText(resultItem.getMessage());
		markText(resultItem);
	}
	
	private void markText(LTLCheckingResultItem resultItem) {
		final JSObject editor = (JSObject) engine.executeScript("LtlEditor.cm");
		for(LTLMarker marker : resultItem.getErrorMarkers()) {
			LTLMark mark = marker.getMark();
			int line = mark.getLine() - 1;				
			JSObject from = (JSObject) engine.executeScript("from = {line:" + line +", ch:" + mark.getPos() +"}");
			JSObject to = (JSObject) engine.executeScript("to = {line:" + line +", ch:" + (mark.getPos() + mark.getLength()) +"}");
			JSObject style = (JSObject) engine.executeScript("style = {className:'error-underline'}");
			editor.call("markText", from, to, style);
		}
	}
}
