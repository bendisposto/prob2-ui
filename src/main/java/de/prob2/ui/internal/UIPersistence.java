package de.prob2.ui.internal;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.prob2.ui.consoles.ConsoleInstruction;
import de.prob2.ui.consoles.ConsoleInstructionOption;
import de.prob2.ui.consoles.groovy.GroovyInterpreter;
import de.prob2.ui.consoles.groovy.objects.GroovyObjectItem;
import de.prob2.ui.consoles.groovy.objects.GroovyObjectStage;
import de.prob2.ui.menu.DetachViewStage;
import de.prob2.ui.menu.MenuController;

import javafx.stage.Stage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public final class UIPersistence {
	private static final Logger LOGGER = LoggerFactory.getLogger(UIPersistence.class);
	
	private final DetachViewStage detachViewStage;
	private final UIState uiState;
	private final Injector injector;
	
	@Inject
	private UIPersistence(final DetachViewStage detachViewStage, final UIState uiState, final Injector injector) {
		this.detachViewStage = detachViewStage;
		this.uiState = uiState;
		this.injector = injector;
	}
	
	private void restoreStage(final String id) {
		LOGGER.info("Restoring stage with ID {}", id);
		if (id == null) {
			LOGGER.warn("Stage identifier is null, cannot restore window");
			return;
		}
		
		switch (id) {
			case "de.prob2.ui.consoles.groovy.objects.GroovyObjectStage":
				injector.getInstance(GroovyInterpreter.class).exec(new ConsoleInstruction("inspect", ConsoleInstructionOption.ENTER));
				return;
			
			case "de.prob2.ui.operations.OperationsView":
			case "de.prob2.ui.history.HistoryView":
			case "de.prob2.ui.modelchecking.ModelcheckingController":
			case "de.prob2.ui.stats.StatsView":
			case "de.prob2.ui.animations.AnimationsView":
				detachViewStage.detach(id);
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
	}
	
	public void open() {
		injector.getInstance(MenuController.class).loadPreset(uiState.getGuiState());
		for (final String id : uiState.getStages()) {
			this.restoreStage(id);
		}
	}
}
