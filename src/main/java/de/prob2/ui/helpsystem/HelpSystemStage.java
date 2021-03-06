package de.prob2.ui.helpsystem;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ResourceBundle;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.prob.Main;

import de.prob2.ui.ProB2;
import de.prob2.ui.internal.StageManager;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;

@Singleton
public class HelpSystemStage extends Stage {
	HelpSystem help;

	@Inject
	private HelpSystemStage(final StageManager stageManager, ResourceBundle bundle, final Injector injector) throws URISyntaxException, IOException{
		this.setTitle(bundle.getString("helpsystem.stage.title"));
		help = new HelpSystem(stageManager, injector);
		this.setScene(new Scene(help));
		stageManager.register(this, this.getClass().getName());
		String defaultDir;
		if (help.isJar)
			defaultDir = Main.getProBDirectory() + "prob2ui" + File.separator + "help" + File.separator + help.helpSubdirectoryString + File.separator;
		else
			defaultDir = ProB2.class.getClassLoader().getResource("help" + File.separator + help.helpSubdirectoryString + File.separator).toString();
		File defaultPage = new File(defaultDir + "ProB2UI.html");
		setContent(defaultPage,"");
	}

	public void setContent(File file, String anchor) {
		String uri = file.toURI().toString();
		int lastIndex = uri.lastIndexOf("file:/");
		Platform.runLater(() ->	((HelpSystem) this.getScene().getRoot()).webEngine.load(uri.substring(lastIndex).replace("%25","%") + anchor));
	}
}
