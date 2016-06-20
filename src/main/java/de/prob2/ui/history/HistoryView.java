package de.prob2.ui.history;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.UUID;

import com.google.inject.Inject;

import de.prob.animator.domainobjects.FormulaExpand;
import de.prob.statespace.Animations;
import de.prob.statespace.ITraceChangesListener;
import de.prob.statespace.Trace;
import de.prob.statespace.Transition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TitledPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.transform.Scale;
import javafx.stage.Stage;

public class HistoryView extends TitledPane implements Initializable, ITraceChangesListener {

	@FXML
	private ListView<Object> lv_history;

	@FXML
	private ToggleButton tb_reverse;

	@FXML
	private Button btprevious;

	@FXML
	private Button btforward;
	
	@FXML
	private Button btshowgraph;

	private boolean rootatbottom = true;

	private Animations animations;

	@Inject
	public HistoryView(FXMLLoader loader, Animations animations) {
		this.animations = animations;
		animations.registerAnimationChangeListener(this);

		try {
			loader.setLocation(getClass().getResource("history_view.fxml"));
			loader.setRoot(this);
			loader.setController(this);
			loader.load();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void initialize(URL location, ResourceBundle resources) {

		animations.registerAnimationChangeListener(this);
		lv_history.setOnMouseClicked(e -> {

			animations.traceChange(animations.getTraces().get(0).gotoPosition(getCurrentIndex()));

		});

		lv_history.setOnMouseMoved(e -> {
			lv_history.setCursor(Cursor.HAND);
		});

		tb_reverse.setOnAction(e -> {
			Collections.reverse(lv_history.getItems());
			rootatbottom = !rootatbottom;
			animations.traceChange(animations.getTraces().get(0).gotoPosition(getCurrentIndex()));
		});

		btprevious.setOnAction(e -> {
			animations.traceChange(animations.getTraces().get(0).back());
		});

		btforward.setOnAction(e -> {
			animations.traceChange(animations.getTraces().get(0).forward());
		});
		
		
		btshowgraph.setOnAction(e -> {
			String url = getClass().getResource("/glyphicons_free/glyphicons/png/glyphicons-9-film.png").toString();
			ScrollPane pane = new ScrollPane();
			Stage stage = new Stage();
			ImageView graph = new ImageView(new Image(url));
			graph.setFitHeight(1000);
			graph.setFitWidth(1000);
			graph.setOnMouseClicked(graphe -> {
				if(graphe.getButton() == MouseButton.PRIMARY) {
					graph.setFitHeight(graph.getFitHeight() * 2);
					graph.setFitWidth(graph.getFitWidth() * 2);
				} else if(graphe.getButton() == MouseButton.SECONDARY) {
					graph.setFitHeight(graph.getFitHeight() * 0.5);
					graph.setFitWidth(graph.getFitWidth() * 0.5);
				}
			    pane.setContent(graph); 
			});
			
			pane.setContent(graph); 
			stage.setTitle("Dotty");
			Scene scene = new Scene(pane, 800, 600);
			stage.setScene(scene);
			stage.show();
		});

	}

	private String extractPrettyName(final String name) {
		if ("$setup_constants".equals(name)) {
			return "SETUP_CONSTANTS";
		}
		if ("$initialise_machine".equals(name)) {
			return "INITIALISATION";
		}
		return name;
	}

	@Override
	public void changed(List<Trace> t) {
		if (lv_history == null) {
			return;
		}
		lv_history.getItems().clear();
		if (t == null || t.isEmpty()) {
			return;
		}
		try {

			int currentPos = t.get(0).getCurrent().getIndex();
			List<Transition> opList = t.get(0).getTransitionList();
			int startpos = 0;
			int endpos = opList.size();

			lv_history.getItems().add("---root---");
			t.get(0).getStateSpace().evaluateTransitions(opList.subList(startpos, endpos), FormulaExpand.truncate);

			for (int i = startpos; i < endpos; i++) {
				Text rep = new Text(opList.get(i).getPrettyRep());
				if (i > currentPos) {
					rep.setFont(Font.font("ARIAL", FontPosture.ITALIC, 12));
					rep.setFill(Color.GRAY);
				} else if (i == currentPos) {
					rep.setFont(Font.font("ARIAL", FontWeight.BOLD, 12));
				}
				lv_history.getItems().add(rep);
			}

			if (rootatbottom) {
				Collections.reverse(lv_history.getItems());
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private int getCurrentIndex() {
		int currentPos = lv_history.getSelectionModel().getSelectedIndex();
		int length = lv_history.getItems().size();
		int index = 0;
		if (rootatbottom) {
			index = length - 2 - currentPos;
		} else {
			index = currentPos - 1;
		}
		return index;

	}

	@Override
	public void removed(List<UUID> t) {
		// TODO Auto-generated method stub

	}

	@Override
	public void animatorStatus(Set<UUID> busy) {
		// TODO Auto-generated method stub

	}

}
