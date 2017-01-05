package de.prob2.ui;

import java.util.HashMap;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob2.ui.internal.StageManager;
import de.prob2.ui.internal.UIState;

import javafx.fxml.FXML;
import javafx.scene.control.Accordion;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.BorderPane;

@Singleton
public class MainController extends BorderPane {
	
	private StageManager stageManager;
	
	@FXML
	private Accordion leftAccordion;
	
	@FXML
	private Accordion rightAccordion;
	
	//If the user creates his own FXML and wants and accordion at the top
	@FXML
	private Accordion topAccordion;
	
	//If the user creates his own FXML and wants and accordion at the bottom
	@FXML
	private Accordion bottomAccordion;
	
	@FXML
	private TitledPane operationsTP;
	
	@FXML
	private TitledPane modelcheckTP;
	
	@FXML
	private TitledPane historyTP;
	
	@FXML
	private TitledPane animationsTP;
	
	@FXML
	private TitledPane statsTP;
	
	private final UIState uiState;
	
	@Inject
	public MainController(StageManager stageManager, UIState uiState) {
		this.stageManager = stageManager;
		this.uiState = uiState;
		refresh();
	}
	
	public void refresh() {
		String guiState = "main.fxml";
		if (!"detached".equals(uiState.getGuiState())) {
			guiState = uiState.getGuiState();
		}
		stageManager.loadFXML(this, guiState);
	}
	
	@FXML
	public void operationsTPClicked() {
		handleTitledPaneClicked(operationsTP);
	}
	
	@FXML
	public void historyTPClicked() {
		handleTitledPaneClicked(historyTP);
	}
	
	@FXML
	public void modelcheckTPClicked() {
		handleTitledPaneClicked(modelcheckTP);
	}
	
	@FXML
	public void statsTPClicked() {
		handleTitledPaneClicked(statsTP);
	}
		
	@FXML
	public void animationsTPClicked() {
		handleTitledPaneClicked(animationsTP);
	}
	
	public void handleTitledPaneClicked(TitledPane pane) {
		for (TitledPane titledPane : ((Accordion) pane.getParent()).getPanes()) {
			uiState.getExpandedTitledPanes().remove(titledPane.getText());
		}
		if (pane.isExpanded()) {
			uiState.getExpandedTitledPanes().add(pane.getText());
		} else {
			uiState.getExpandedTitledPanes().remove(pane.getText());
		}
	}
	
	public void expandTitledPane(String titledPane) {
		HashMap<String,TitledPane> titledPanes = new HashMap<>();
		titledPanes.put("Operations", operationsTP);
		titledPanes.put("History", historyTP);
		titledPanes.put("Animations", animationsTP);
		titledPanes.put("Model Check", modelcheckTP);
		titledPanes.put("Statistics", statsTP);


		if (!titledPanes.containsKey(titledPane)) {
			return;
		}
		for (Accordion accordion : new Accordion[] {leftAccordion, rightAccordion, bottomAccordion, topAccordion}) {
			if (accordion == null) {
				continue;
			}
			if (accordion.getPanes().contains(titledPanes.get(titledPane))) {
				accordion.setExpandedPane(titledPanes.get(titledPane));
			}
		}
	}
}
