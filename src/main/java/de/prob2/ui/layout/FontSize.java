package de.prob2.ui.layout;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;

@Singleton
public final class FontSize {
	public static final int DEFAULT_FONT_SIZE = 13;
	
	private final IntegerProperty size;
	
	@Inject
	private FontSize() {
		this.size = new SimpleIntegerProperty(this, "fontSize", DEFAULT_FONT_SIZE);
		this.size.addListener((o, from, to) -> {
			if (to.intValue() <= 1) {
				this.setFontSize(2);
			}
		});
	}
	
	public IntegerProperty fontSizeProperty() {
		return this.size;
	}
	
	public int getFontSize() {
		return this.fontSizeProperty().get();
	}
	
	public void setFontSize(final int fontSize) {
		this.fontSizeProperty().set(fontSize);
	}
	
	public void resetFontSize() {
		this.setFontSize(DEFAULT_FONT_SIZE);
	}
}
