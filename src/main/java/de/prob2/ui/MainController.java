package de.prob2.ui;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob2.ui.internal.StageManager;
import de.prob2.ui.internal.UIState;

import javafx.scene.layout.BorderPane;

@Singleton
public class MainController extends BorderPane {
	private StageManager stageManager;
	
	@Inject
	public MainController(StageManager stageManager, UIState uiState) {
		this.stageManager = stageManager;
		refresh(uiState);
	}
	
	public void refresh(UIState uiState) {
		stageManager.loadFXML(this, uiState.getGuiState());
	}
}
