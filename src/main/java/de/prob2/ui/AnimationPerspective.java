package de.prob2.ui;

import java.util.HashMap;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob2.ui.animations.AnimationsView;
import de.prob2.ui.history.HistoryView;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.modelchecking.ModelcheckingController;
import de.prob2.ui.operations.OperationsView;
import de.prob2.ui.stats.StatsView;

import javafx.fxml.FXML;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Accordion;
import javafx.scene.control.TitledPane;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public final class AnimationPerspective extends BorderPane {
	private static final Logger logger = LoggerFactory.getLogger(AnimationPerspective.class);
	
	@FXML
	private OperationsView operations;
	@FXML
	private TitledPane operationsTP;
	@FXML
	private HistoryView history;
	@FXML
	private TitledPane historyTP;
	@FXML
	private ModelcheckingController modelcheck;
	@FXML
	private TitledPane modelcheckTP;
	@FXML
	private StatsView stats;
	@FXML
	private TitledPane statsTP;
	@FXML
	private AnimationsView animations;
	@FXML
	private TitledPane animationsTP;
	@FXML
	private Accordion leftAccordion;
	@FXML
	private Accordion rightAccordion;
	@FXML
	private Accordion topAccordion;
	@FXML
	private Accordion bottomAccordion;

	private boolean dragged;
	private ImageView snapshot = new ImageView();

	private HashMap<Node, TitledPane> nodeMap = new HashMap<>();

	@Inject
	private AnimationPerspective(StageManager stageManager) {
		stageManager.loadFXML(this, "animation_perspective.fxml");
	}

	@FXML
	public void initialize() {
		double initialHeight = 200;
		double initialWidth = 280;
		operations.setPrefSize(initialWidth,initialHeight);
		history.setPrefSize(initialWidth,initialHeight);
		modelcheck.setPrefSize(initialWidth,initialHeight);
		animations.setPrefSize(initialWidth,initialHeight);
		nodeMap.put(operations,operationsTP);
		nodeMap.put(history,historyTP);
		nodeMap.put(modelcheck,modelcheckTP);
		nodeMap.put(animations,animationsTP);
		nodeMap.put(stats,statsTP);
		onDrag();
	}

	private void onDrag() {
		nodeMap.keySet().forEach(this::registerDrag);
	}

	private void registerDrag(final Node node) {
		node.setOnMouseEntered(mouseEvent -> this.setCursor(Cursor.OPEN_HAND));
		node.setOnMousePressed(mouseEvent -> this.setCursor(Cursor.CLOSED_HAND));
		node.setOnMouseDragEntered(mouseEvent -> {
			dragged = true;
			mouseEvent.consume();
		});
		node.setOnMouseDragged(mouseEvent -> {
			Point2D position = this.sceneToLocal(new Point2D(mouseEvent.getSceneX(), mouseEvent.getSceneY()));
			snapshot.relocate(position.getX(), position.getY());
			mouseEvent.consume();
		});
		node.setOnMouseReleased(mouseEvent -> {
			this.setCursor(Cursor.DEFAULT);
			if (dragged){
				dragDropped(node,mouseEvent);
			}
			dragged = false;
			snapshot.setImage(null);
			if (((BorderPane) this.getParent()).getChildren().contains(snapshot)) {
				((BorderPane) this.getParent()).getChildren().remove(snapshot);
			}
			mouseEvent.consume();
		});
		node.setOnDragDetected(mouseEvent -> {
			node.startFullDrag();
			SnapshotParameters snapParams = new SnapshotParameters();
			snapParams.setFill(Color.TRANSPARENT);
			snapshot.setImage(node.snapshot(snapParams,null));
			snapshot.setFitWidth(200);
			snapshot.setPreserveRatio(true);
			((BorderPane) this.getParent()).getChildren().add(snapshot);
			mouseEvent.consume();
		});
	}

	private void dragDropped(final Node node, MouseEvent mouseEvent){
		TitledPane nodeTP = nodeMap.get(node);
		Accordion oldParent = (Accordion) nodeTP.getParent();
		Point2D position = this.sceneToLocal(new Point2D(mouseEvent.getSceneX(), mouseEvent.getSceneY()));

		boolean middleX = position.getX() > this.getScene().getWidth()/3 && position.getX() <= 2*this.getScene().getWidth()/3;
		boolean middleY = position.getY() > this.getScene().getHeight()/3 && position.getY() <= 2*this.getScene().getHeight()/3;

		boolean right = position.getX() > this.getScene().getWidth()/2;
		boolean bottom = position.getY() > this.getScene().getHeight()/2;

		boolean switchToRight = right && middleY && !rightAccordion.getPanes().contains(nodeTP);
		boolean switchToTop = !bottom && middleX && !topAccordion.getPanes().contains(nodeTP);
		boolean switchToBottom = bottom && middleX && !bottomAccordion.getPanes().contains(nodeTP);
		boolean switchToLeft = !right && middleY && !leftAccordion.getPanes().contains(nodeTP);

		Accordion newParent = calculateNewParent(switchToTop,switchToBottom,switchToLeft,switchToRight,nodeTP);

		if (newParent != null) {
			switchParent(oldParent, newParent, nodeTP);
		}
	}

	private Accordion calculateNewParent(
			boolean switchToTop,
			boolean switchToBottom,
			boolean switchToLeft,
			boolean switchToRight,
			TitledPane nodeTP
	) {
		if (switchToRight) {
			nodeTP.setCollapsible(false);
			return rightAccordion;
		} else if (switchToTop) {
			nodeTP.setCollapsible(true);
			return topAccordion;
		} else if (switchToBottom) {
			nodeTP.setCollapsible(true);
			return bottomAccordion;
		} else if (switchToLeft) {
			nodeTP.setCollapsible(false);
			return leftAccordion;
		} else {
			return null;
		}
	}

	private void switchParent(Accordion oldParent, Accordion newParent, TitledPane nodeTP){
		oldParent.getPanes().remove(nodeTP);
		if (!oldParent.getPanes().isEmpty()) {
			oldParent.setExpandedPane(oldParent.getPanes().get(0));
			if (oldParent != bottomAccordion && oldParent != topAccordion && oldParent.getPanes().size()==1){
				oldParent.getPanes().get(0).setCollapsible(false);
			}
		}

		if (newParent.getPanes().size()>=1){
			for (TitledPane panes:newParent.getPanes()){
				panes.setCollapsible(true);
				panes.setExpanded(false);
			}
			nodeTP.setCollapsible(true);
		}
		newParent.getPanes().add(nodeTP);
		newParent.setExpandedPane(nodeTP);
	}
	
}
