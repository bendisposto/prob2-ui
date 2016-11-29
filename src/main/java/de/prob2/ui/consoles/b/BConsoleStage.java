package de.prob2.ui.consoles.b;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob2.ui.internal.StageManager;

import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

@Singleton
public final class BConsoleStage extends Stage {
	@Inject
	private BConsoleStage(StageManager stageManager, BConsole bConsole) {
		this.setTitle("B Console");
		this.setScene(new Scene(new StackPane(bConsole), 800, 600));
		stageManager.register(this);
	}

}
