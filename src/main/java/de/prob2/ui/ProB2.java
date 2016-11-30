package de.prob2.ui;

import com.google.inject.Guice;
import com.google.inject.Injector;

import de.prob.cli.ProBInstanceProvider;

import de.prob2.ui.config.Config;
import de.prob2.ui.internal.ProB2Module;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.internal.UIPersistence;
import de.prob2.ui.internal.UIState;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ProB2 extends Application {
	private Injector injector;
	private Config config;
	
	public static void main(String... args) {
		launch(args);
	}

	@Override
	public void start(Stage stage) throws Exception {
		ProB2Module module = new ProB2Module();
		injector = Guice.createInjector(com.google.inject.Stage.PRODUCTION, module);
		config = injector.getInstance(Config.class);
		UIState uiState = injector.getInstance(UIState.class);
		FXMLLoader loader = injector.getInstance(FXMLLoader.class);
		loader.setLocation(getClass().getResource("main.fxml"));
		loader.load();
		Parent root = loader.getRoot();

		Scene mainScene = new Scene(root, 1024, 768);
		stage.setTitle("ProB 2.0");
		stage.setScene(mainScene);
		stage.setOnHidden(e -> Platform.exit());
		
		// No persistence needed for the main stage, because it is created automatically
		injector.getInstance(StageManager.class).register(stage, null);
		stage.show();
		new UIPersistence(uiState, injector).open();
	}
	
	@Override
	public void stop() {
		config.save();
		injector.getInstance(ProBInstanceProvider.class).shutdownAll();
	}

}
