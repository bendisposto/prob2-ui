package de.prob2.ui.states;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.inject.Inject;

import difflib.DiffUtils;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToggleGroup;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FullValueStage extends Stage {
	private static final Pattern PRETTIFY_DELIMITERS = Pattern.compile("[\\{\\}\\,]");
	private static final Logger logger = LoggerFactory.getLogger(FullValueStage.class);
	
	@FXML private TabPane tabPane;
	@FXML private Tab currentValueTab;
	@FXML private Tab previousValueTab;
	@FXML private Tab diffTab;
	@FXML private TextArea currentValueTextarea;
	@FXML private TextArea previousValueTextarea;
	@FXML private TextArea diffTextarea;
	@FXML private ToggleGroup asciiUnicodeGroup;
	@FXML private RadioButton asciiRadio;
	@FXML private RadioButton unicodeRadio;
	@FXML private CheckBox prettifyCheckBox;
	@FXML private Button saveAsButton;
	
	private AsciiUnicodeString currentValue;
	private AsciiUnicodeString previousValue;
	private String diff;
	
	@Inject
	public FullValueStage(final FXMLLoader loader) {
		loader.setLocation(getClass().getResource("full_value_stage.fxml"));
		loader.setRoot(this);
		loader.setController(this);
		try {
			loader.load();
		} catch (IOException e) {
			logger.error("loading fxml failed", e);
		}
	}
	
	private static String prettify(final String s) {
		final StringBuilder out = new StringBuilder();
		int indentLevel = 0;
		int lastMatchPos = 0;
		Matcher matcher = PRETTIFY_DELIMITERS.matcher(s);
		
		while (matcher.find()) {
			for (int i = 0; i < indentLevel; i++) {
				out.append("\t");
			}
			out.append(s, lastMatchPos, matcher.start());
			lastMatchPos = matcher.end();
			
			switch (matcher.group()) {
				case "{":
					out.append("{\n");
					indentLevel++;
					break;
				
				case "}":
					indentLevel--;
					if (s.charAt(matcher.start()-1) != '{') {
						out.append("\n");
					}
					out.append("}\n");
					break;
				
				case ",":
					out.append(",\n");
					break;
				
				default:
					throw new IllegalStateException("Unhandled delimiter: " + matcher.group());
			}
		}
		
		out.append(s, lastMatchPos, s.length());
		
		return out.toString();
	}
	
	private String prettifyIfEnabled(final String s) {
		return this.prettifyCheckBox.isSelected() ? prettify(s) : s;
	}
	
	public AsciiUnicodeString getCurrentValue() {
		return this.currentValue;
	}
	
	public void setCurrentValue(final AsciiUnicodeString currentValue) {
		Objects.requireNonNull(currentValue);
		this.currentValue = currentValue;
		this.updateText();
	}
	
	public String currentValueAsString() {
		return asciiRadio.isSelected() ? this.getCurrentValue().toAscii() : this.getCurrentValue().toUnicode();
	}
	
	public AsciiUnicodeString getPreviousValue() {
		return this.previousValue;
	}
	
	public void setPreviousValue(final AsciiUnicodeString previousValue) {
		Objects.requireNonNull(previousValue);
		this.previousValue = previousValue;
		this.updateText();
	}
	
	public String previousValueAsString() {
		return asciiRadio.isSelected() ? this.getPreviousValue().toAscii() : this.getPreviousValue().toUnicode();
	}
	
	@FXML
	private void updateText() {
		final String cv = this.getCurrentValue() == null ? null : prettifyIfEnabled(this.currentValueAsString());
		final String pv = this.getPreviousValue() == null ? null : prettifyIfEnabled(this.previousValueAsString());
		if (cv != null) {
			this.currentValueTextarea.setText(cv);
		}
		if (pv != null) {
			this.previousValueTextarea.setText(pv);
		}
		if (cv != null && pv != null) {
			final List<String> prevLines = Arrays.asList(pv.split("\n"));
			final List<String> curLines = Arrays.asList(cv.split("\n"));
			final List<String> uniDiffLines = DiffUtils.generateUnifiedDiff("", "", prevLines, DiffUtils.diff(prevLines, curLines), 3);
			// Don't display the "file names" in the first two lines
			this.diff = String.join("\n", uniDiffLines.subList(2, uniDiffLines.size()));
			this.diffTextarea.setText(this.diff);
		}
	}
	
	@FXML
	private void saveAs() {
		final FileChooser chooser = new FileChooser();
		chooser.getExtensionFilters().setAll(
			new FileChooser.ExtensionFilter("Text Files", "*.txt")
		);
		chooser.setInitialFileName(this.getTitle() + ".txt");
		final File selected = chooser.showSaveDialog(this);
		if (selected == null) {
			return;
		}
		
		try (final Writer out = new OutputStreamWriter(new FileOutputStream(selected), Charset.forName("UTF-8"))) {
			final String value;
			if (currentValueTab.isSelected()) {
				value = this.currentValueAsString();
			} else if (previousValueTab.isSelected()) {
				value = this.previousValueAsString();
			} else if (diffTab.isSelected()) {
				value = this.diff;
			} else {
				logger.error("No known tab selected");
				return;
			}
			out.write(value);
		} catch (FileNotFoundException e) {
			logger.error("Could not open file for writing", e);
			new Alert(Alert.AlertType.ERROR, "Could not open file for writing:\n" + e.getMessage()).showAndWait();
		} catch (IOException e) {
			logger.error("Failed to save value to file", e);
			new Alert(Alert.AlertType.ERROR, "Failed to save file:\n" + e.getMessage()).showAndWait();
		}
	}
}
