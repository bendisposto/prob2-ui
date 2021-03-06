package de.prob2.ui.visualisation.magiclayout;

import java.util.Objects;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.paint.Color;

public abstract class MagicComponent {
	
	private final StringProperty name = new SimpleStringProperty();
	private final StringProperty expression = new SimpleStringProperty();
	
	private final ObjectProperty<MagicLineType> lineType = new SimpleObjectProperty<>();
	private final ObjectProperty<Color> lineColor = new SimpleObjectProperty<>();
	private final ObjectProperty<MagicLineWidth> lineWidth = new SimpleObjectProperty<>();
	private final ObjectProperty<Color> textColor = new SimpleObjectProperty<>();
	
	public MagicComponent(String name) {
		this(name, "");
	}
	
	public MagicComponent(String name, String expression) {
		this.name.set(name);
		this.expression.set(expression);
		
		this.lineType.set(MagicLineType.CONTINUOUS);
		this.lineColor.set(Color.BLACK);
		this.lineWidth.set(MagicLineWidth.DEFAULT);
		this.textColor.set(Color.BLACK);
	}
	
	public MagicComponent(MagicComponent component) {
		this.name.set(component.getName());
		this.expression.set(component.getExpression());
		
		this.lineType.set(component.getLineType());
		this.lineColor.set(component.getLineColor());
		this.lineWidth.set(component.getLineWidth());
		this.textColor.set(component.getTextColor());
	}
	
	public StringProperty nameProperty() {
		return name;
	}
	
	public String getName() {
		return name.get();
	}
	
	public StringProperty expressionProperty() {
		return expression;
	}
	
	public String getExpression() {
		return expression.get();
	}
	
	public ObjectProperty<MagicLineType> lineTypeProperty() {
		return lineType;
	}
	
	public MagicLineType getLineType() {
		return lineType.get();
	}
	
	public ObjectProperty<Color> lineColorProperty() {
		return lineColor;
	}
	
	public Color getLineColor() {
		return lineColor.get();
	}
	
	public ObjectProperty<MagicLineWidth> lineWidthProperty() {
		return lineWidth;
	}
	
	public MagicLineWidth getLineWidth() {
		return lineWidth.get();
	}

	public ObjectProperty<Color> textColorProperty() {
		return textColor;
	}

	public Color getTextColor() {
		return textColor.get();
	}
	
	@Override
	public String toString() {
		return name.get();
	}
	
	@Override
	public boolean equals(Object other) {
		if (other == this) {
			return true;
		}
		if (!(other instanceof MagicComponent)) {
			return false;
		}
		MagicComponent otherComponent = (MagicComponent) other;
		return otherComponent.name.get().equals(this.name.get());
	}
	
	@Override
	public int hashCode() {
		 return Objects.hash(name.get());
	}

	public void unbindAll() {
		expression.unbind();
		lineType.unbind();
		lineColor.unbind();
		lineWidth.unbind();
		textColor.unbind();
	}
}
