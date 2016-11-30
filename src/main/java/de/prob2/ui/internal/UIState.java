package de.prob2.ui.internal;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class UIState {
	private static final Logger LOGGER = LoggerFactory.getLogger(UIState.class);
	
	private String guiState;
	
	private Set<String> stages;
	
	@Inject
	public UIState() {
		this.guiState = "main.fxml";
		this.stages = new HashSet<>();
	}
	
	public void setGuiState(String guiState) {
		this.guiState = guiState;
	}
	
	public String getGuiState() {
		return guiState;
	}
	
	public void addStage(String stage) {
		Objects.requireNonNull(stage);
		stages.add(stage);
	}
	
	public void removeStage(String stage) {
		Objects.requireNonNull(stage);
		stages.remove(stage);
	}
	
	public Set<String> getStages() {
		return stages;
	}

}
