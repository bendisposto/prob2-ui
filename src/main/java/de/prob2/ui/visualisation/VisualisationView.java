package de.prob2.ui.visualisation;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob.statespace.State;
import de.prob.statespace.StateSpace;
import de.prob2.ui.commands.GetImagesForMachineCommand;
import de.prob2.ui.commands.GetImagesForStateCommand;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import javafx.fxml.FXML;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;

@Singleton
public class VisualisationView extends AnchorPane {
	@FXML
	private StackPane probLogoStackPane;
	@FXML
	private ScrollPane visualisationScrollPane;
	@FXML
	private GridPane visualisationGridPane;

	private final CurrentTrace currentTrace;
	private final CurrentProject currentProject;

	@Inject
	public VisualisationView(final CurrentTrace currentTrace, final CurrentProject currentProject,
			final StageManager stageManager) {
		this.currentTrace = currentTrace;
		this.currentProject = currentProject;
		stageManager.loadFXML(this, "visualisation_view.fxml");
	}

	@FXML
	public void initialize() {
		visualisationScrollPane.visibleProperty().bind(probLogoStackPane.visibleProperty().not());
		
		currentTrace.currentStateProperty().addListener((observable, from, to) -> {
			visualisationGridPane.getChildren().clear();

			if (to != null && to.isInitialised()) {
				StateSpace stateSpace = to.getStateSpace();

				GetImagesForMachineCommand getImagesForMachineCommand = new GetImagesForMachineCommand();
				stateSpace.execute(getImagesForMachineCommand);
				Map<Integer, String> images = getImagesForMachineCommand.getImages();

				if (!images.isEmpty()) {
					probLogoStackPane.setVisible(false);
					showImages(to, stateSpace, images);
				} else {
					probLogoStackPane.setVisible(true);
				}
			} else {
				probLogoStackPane.setVisible(true);
			}
		});

	}

	private void showImages(State state, StateSpace stateSpace, Map<Integer, String> images) {
		GetImagesForStateCommand getImagesForStateCommand = new GetImagesForStateCommand(state.getId());
		stateSpace.execute(getImagesForStateCommand);
		int[][] imageMatrix = getImagesForStateCommand.getMatrix();
		int rowNr = getImagesForStateCommand.getRows();
		int columnNr = getImagesForStateCommand.getColumns();

		for (int r = 0; r < rowNr; r++) {
			for (int c = 0; c < columnNr; c++) {
				String imageURL = images.get(imageMatrix[r][c]);
				final String projectLocation = currentProject.get().getLocation().getPath();
				Path imagePath = Paths.get(projectLocation, imageURL);
				ImageView imageView = new ImageView(new Image("file:" + imagePath.toString()));
				visualisationGridPane.add(imageView, c, r);
			}
		}
	}
}