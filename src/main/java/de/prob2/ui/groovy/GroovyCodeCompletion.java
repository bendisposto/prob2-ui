package de.prob2.ui.groovy;

import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ListView;
import javafx.scene.shape.Path;
import javafx.stage.Popup;

public class GroovyCodeCompletion {
	
	private Popup popup;
	
	private ListView<String> lv_suggestions;

	
	//Trying with ListView
	public GroovyCodeCompletion() {
		popup = new Popup();
		lv_suggestions = new ListView<String>();
		lv_suggestions.getItems().add("boo");
		lv_suggestions.getItems().add("boo");
		lv_suggestions.getItems().add("boo");
		lv_suggestions.getItems().add("boo");
		lv_suggestions.getItems().add("boo");
		lv_suggestions.getItems().add("boo");
		lv_suggestions.setMaxHeight(200);
		lv_suggestions.setMaxWidth(400);
		lv_suggestions.setPrefHeight(200);
		lv_suggestions.setPrefWidth(400);
		popup.getContent().add(lv_suggestions);
		
		
	}
	
	public void activate(GroovyConsole console) {
		Point2D point = findCaretPosition(findCaret(console));
		double x = point.getX() + 10;
		double y = point.getY() + 10;
		popup.show(console, x, y);
		
	}
	
	public void deactivate() {
		popup.hide();
	}
	
	public boolean isVisible() {
		return popup.isShowing();
	}
	
	
	private Path findCaret(Parent parent) {
		for (Node node : parent.getChildrenUnmodifiable()) {
			if (node instanceof Path) {
				return (Path) node;
			} else if (node instanceof Parent) {
				Path caret = findCaret((Parent) node);
				if (caret != null) {
					return caret;
				}
			}
		}
		return null;
	}
	

	private Point2D findCaretPosition(Node node) {
		double x = 0;
		double y = 0;
		if(node == null) {
			return null;
		}
		for (Node n = node; n != null; n=n.getParent()) {
			Bounds parentBounds = n.getBoundsInParent();
			x += parentBounds.getMinX();
			y += parentBounds.getMinY();
		}
		if(node.getScene() != null) {
			Scene scene = node.getScene();
			x += scene.getX() + scene.getWindow().getX();
			y += scene.getY() + scene.getWindow().getY();
			x = Math.min(scene.getWindow().getX() + scene.getWindow().getWidth() - 20, x);
			y = Math.min(scene.getWindow().getY() + scene.getWindow().getHeight() - 20, y);
		}
		return new Point2D(x,y);
	}

}
