package de.prob2.ui.internal;

import com.google.inject.Injector;

import de.prob2.ui.consoles.ConsoleInstruction;
import de.prob2.ui.consoles.ConsoleInstructionOption;
import de.prob2.ui.consoles.groovy.GroovyInterpreter;
import de.prob2.ui.consoles.groovy.objects.GroovyObjectItem;
import de.prob2.ui.consoles.groovy.objects.GroovyObjectStage;
import de.prob2.ui.menu.MenuController;
import de.prob2.ui.preferences.PreferencesStage;

import javafx.stage.Stage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UIPersistence {
	private static final Logger LOGGER = LoggerFactory.getLogger(UIPersistence.class);

	private UIState uiState;
	
	private Injector injector;
	
	public UIPersistence(UIState uiState, Injector injector) {
		this.uiState = uiState;
		this.injector = injector;
	}
	
	private void restoreStage(final String id, final MenuController menuController) {
		LOGGER.info("Restoring stage with ID {}", id);
		if (id == null) {
			LOGGER.warn("Stage identifier is null, cannot restore window");
			return;
		}
		
		switch (id) {
			case "de.prob2.ui.consoles.groovy.objects.GroovyObjectStage":
				injector.getInstance(GroovyInterpreter.class).exec(new ConsoleInstruction("inspect", ConsoleInstructionOption.ENTER));
				return;
			
			case "de.prob2.ui.menu.MenuController$DetachViewStageController":
				injector.getInstance(MenuController.class).handleLoadDetached();
				return;
			
			case "de.prob2.ui.operations.OperationsView":
			case "de.prob2.ui.history.HistoryView":
			case "de.prob2.ui.modelchecking.ModelcheckingController":
			case "de.prob2.ui.stats.StatsView":
			case "de.prob2.ui.animations.AnimationsView":
				menuController.detach(id);
				return;
			
			default:
				LOGGER.info("No special handling for stage identifier {}, will use injection", id);
		}
		
		Class<?> clazz;
		try {
			clazz = Class.forName(id);
		} catch (ClassNotFoundException e) {
			LOGGER.warn("Class not found, cannot restore window", e);
			return;
		}
		
		Class<? extends Stage> stageClazz;
		try {
			stageClazz = clazz.asSubclass(Stage.class);
		} catch (ClassCastException e) {
			LOGGER.warn("Class is not a subclass of javafx.stage.Stage, cannot restore window", e);
			return;
		}
		
		try {
			injector.getInstance(stageClazz).show();
		} catch (RuntimeException e) {
			LOGGER.warn("Failed to restore window", e);
		}
		for (GroovyObjectItem item: injector.getInstance(GroovyObjectStage.class).getItems()) {
			if(uiState.getStages().contains(item.getClazzname())) {
				item.show();
			}
		}
		PreferencesStage preferencesStage = injector.getInstance(PreferencesStage.class);
		switch(preferencesStage.getCurrentTab()) {
			case "ProB Preferences":
				preferencesStage.selectPreferences();
				break;
			case "States View":
				preferencesStage.selectStatesView();
				break;
			default:
				preferencesStage.selectGeneral();
		}
	}
	
	public void open() {
		MenuController menu = injector.getInstance(MenuController.class);
		menu.loadPreset(uiState.getGuiState());
		for (final String id : uiState.getStages()) {
			this.restoreStage(id, menu);
		}
	}
}
