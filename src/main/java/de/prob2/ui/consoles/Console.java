package de.prob2.ui.consoles;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.stream.Collectors;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.input.Clipboard;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.TransferMode;

import org.fxmisc.richtext.StyleClassedTextArea;
import org.fxmisc.wellbehaved.event.EventPattern;
import org.fxmisc.wellbehaved.event.InputMap;
import org.fxmisc.wellbehaved.event.Nodes;


public abstract class Console extends StyleClassedTextArea {
	private static final Set<KeyCode> REST = EnumSet.of(KeyCode.ESCAPE, KeyCode.SCROLL_LOCK, KeyCode.PAUSE, KeyCode.NUM_LOCK, KeyCode.INSERT, KeyCode.CONTEXT_MENU, KeyCode.CAPS, KeyCode.TAB, KeyCode.ALT);
	
	private static final String EMPTY_PROMPT = "> ";
	
	private final ResourceBundle bundle;
	private final List<ConsoleInstruction> instructions;
	protected int charCounterInLine = 0;
	protected int currentPosInLine = 0;
	protected int posInList = -1;
	protected int instructionLengthInLine = 1;
	private final ConsoleSearchHandler searchHandler;
	private final Executable interpreter;
	private final String header;
	private final StringProperty prompt;
	
	protected Console(ResourceBundle bundle, String header, String prompt, Executable interpreter) {
		this.bundle = bundle;
		this.instructions = new ArrayList<>();
		this.searchHandler = new ConsoleSearchHandler(this, bundle);
		this.interpreter = interpreter;
		this.header = header;
		this.prompt = new SimpleStringProperty(this, "prompt", prompt);
		
		this.requestFollowCaret();
		setEvents();
		setDragDrop();
		this.reset();
		this.setWrapText(true);
		this.getStyleClass().add("console");
		
		this.promptProperty().addListener((o, from, to) -> {
			// If the cursor is in the input, remember its position relative to the end of the prompt, and place it there again after the prompt is updated.
			final int caretPositionInInput;
			if (this.getCaretPosition() >= this.getLineStart()) {
				caretPositionInInput = Math.max(this.getCaretColumn() - from.length(), 0);
			} else {
				caretPositionInInput = -1;
			}
			this.replace(this.getLineNumber(), 0, this.getLineNumber(), from.length(), to, this.getStyleAtPosition(this.getLineNumber(), 0));
			if (caretPositionInInput != -1) {
				this.moveTo(this.getLineNumber(), caretPositionInInput + to.length());
			}
		});
	}
	
	public void setEvents() {
		Nodes.addInputMap(this, InputMap.consume(EventPattern.mouseClicked(MouseButton.PRIMARY), e -> this.mouseClicked()));
		Nodes.addInputMap(this, InputMap.consume(EventPattern.keyPressed(), this::keyPressed));
		
		// GUI-style shortcuts, these should use the Shortcut key (i. e. Command on Mac, Control on other systems).
		Nodes.addInputMap(this, InputMap.consume(EventPattern.keyPressed(KeyCode.C, KeyCombination.SHORTCUT_DOWN), e-> this.copy()));
		Nodes.addInputMap(this, InputMap.consume(EventPattern.keyPressed(KeyCode.V, KeyCombination.SHORTCUT_DOWN), e-> this.paste()));
		
		// Shell/Emacs-style shortcuts, these should always use Control as the modifier, even on Mac (this is how it works in a normal terminal window).
		Nodes.addInputMap(this, InputMap.consume(EventPattern.keyPressed(KeyCode.R, KeyCombination.CONTROL_DOWN), e-> this.controlR()));
		Nodes.addInputMap(this, InputMap.consume(EventPattern.keyPressed(KeyCode.A, KeyCombination.CONTROL_DOWN), e-> this.controlA()));
		Nodes.addInputMap(this, InputMap.consume(EventPattern.keyPressed(KeyCode.E, KeyCombination.CONTROL_DOWN), e-> this.controlE()));
		Nodes.addInputMap(this, InputMap.consume(EventPattern.keyPressed(KeyCode.K, KeyCombination.CONTROL_DOWN), e-> this.reset()));
				
		Nodes.addInputMap(this, InputMap.consume(EventPattern.keyPressed(KeyCode.UP), e-> this.handleUp()));
		Nodes.addInputMap(this, InputMap.consume(EventPattern.keyPressed(KeyCode.DOWN), e-> this.handleDown()));
		Nodes.addInputMap(this, InputMap.consume(EventPattern.keyPressed(KeyCode.LEFT), e-> this.handleLeft()));
		Nodes.addInputMap(this, InputMap.consume(EventPattern.keyPressed(KeyCode.RIGHT), e-> this.handleRight()));
		Nodes.addInputMap(this, InputMap.consume(EventPattern.keyPressed(KeyCode.DELETE), this::handleDeletion));
		Nodes.addInputMap(this, InputMap.consume(EventPattern.keyPressed(KeyCode.BACK_SPACE), this::handleDeletion));
		Nodes.addInputMap(this, InputMap.consume(EventPattern.keyPressed(KeyCode.ENTER, KeyCombination.SHIFT_DOWN), KeyEvent::consume));
		Nodes.addInputMap(this, InputMap.consume(EventPattern.keyPressed(KeyCode.ESCAPE, KeyCombination.SHIFT_DOWN), KeyEvent::consume));
		Nodes.addInputMap(this, InputMap.consume(EventPattern.keyPressed(KeyCode.ENTER, KeyCombination.ALT_DOWN), KeyEvent::consume));
		Nodes.addInputMap(this, InputMap.consume(EventPattern.keyPressed(KeyCode.BACK_SPACE, KeyCombination.ALT_DOWN), KeyEvent::consume));
		Nodes.addInputMap(this, InputMap.consume(EventPattern.keyPressed(KeyCode.DELETE, KeyCombination.ALT_DOWN), KeyEvent::consume));
		Nodes.addInputMap(this, InputMap.consume(EventPattern.keyPressed(KeyCode.ENTER), e-> this.handleEnter()));
	}
	
	private void setDragDrop() {
		this.setOnDragOver(e-> {
			Dragboard dragboard = e.getDragboard();
			if (dragboard.hasFiles()) {
				e.acceptTransferModes(TransferMode.COPY);
			} else {
				e.consume();
			}
		});
		
		this.setOnDragDropped(e-> {
			Dragboard dragboard = e.getDragboard();
			boolean success = false;
			if (dragboard.hasFiles() && this.getCaretPosition() >= this.getInputStart()) {
				success = true;
				for (File file : dragboard.getFiles()) {
					String path = file.getAbsolutePath();
					int caretPosition = this.getCaretPosition();
					this.insertText(this.getCaretPosition(), path);
					charCounterInLine += path.length();
					currentPosInLine += path.length();
					this.moveTo(caretPosition + path.length());
				}
			}
			e.setDropCompleted(success);
			e.consume();
		});
	}
	
	@Override
	public void paste() {
		if (searchHandler.isActive()) {
			return;
		}
		if (this.getLength() - 1 - this.getCaretPosition() >= charCounterInLine) {
			goToLastPos();
		}

		final int oldLength = this.getLength();
		String[] pastedLines = Clipboard.getSystemClipboard().getString().split("\n");
		for(int i = 0; i < pastedLines.length - 1; i++) {
			this.appendText(pastedLines[i] + "\\");
			handleEnter();
		}
		this.appendText(pastedLines[pastedLines.length - 1]);
		final int diff = this.getLength() - oldLength;

		charCounterInLine += diff;
		currentPosInLine += diff;
	}
		
	@Override
	public void copy() {
		super.copy();
		goToLastPos();
	}
					
	private void mouseClicked() {
		if (this.getLength() - 1 - this.getCaretPosition() < charCounterInLine) {
			currentPosInLine = charCounterInLine - (this.getLength() - this.getCaretPosition());
		}
	}
	
	public void controlR() {
		if (searchHandler.isActive()) {
			searchHandler.searchNext();
		} else {
			activateSearch();
		}
	}
	
	protected void keyPressed(KeyEvent e) {
		if (REST.contains(e.getCode())) {
			return;
		}
		if (!e.getCode().isFunctionKey() && !e.getCode().isMediaKey() && !e.getCode().isModifierKey()) {
			handleInsertChar(e);
		}
	}
	
	private void handleInsertChar(KeyEvent e) {
		if (this.getLength() - this.getCaretPosition() > charCounterInLine) {
			goToLastPos();
		}
		if (e.isControlDown() || e.isMetaDown() || e.getText().isEmpty()) {
			return;
		}
		charCounterInLine++;
		currentPosInLine++;
		posInList = instructions.size() - 1;
		searchHandler.handleKey(e);
	}
	
	private void controlA() {
		if (!searchHandler.isActive()) {
			this.moveTo(this.getCaretPosition() - currentPosInLine);
			currentPosInLine = 0;
		}
	}
	
	private void controlE() {
		if (!searchHandler.isActive()) {
			this.moveTo(this.getLength());
			currentPosInLine = charCounterInLine;
		}
	}

	protected void activateSearch() {
		final String input = this.getInput();
		this.deleteText(getLineNumber(), 0, getLineNumber(), this.getParagraphLength(getLineNumber()));
		this.appendText(String.format(bundle.getString("consoles.prompt.backwardSearch"), "", input));
		this.moveTo(getLineNumber(), this.getLine().lastIndexOf('\''));
		currentPosInLine = 0;
		charCounterInLine = 0;
		searchHandler.activateSearch();
	}
	
	protected void deactivateSearch() {
		if (searchHandler.isActive()) {
			String searchResult = searchHandler.getCurrentSearchResult();
			this.deleteText(getLineNumber(), 0, getLineNumber(), this.getParagraphLength(getLineNumber()));
			this.appendText((instructionLengthInLine > 1 ? EMPTY_PROMPT : prompt.get()) + searchResult);
			this.moveTo(this.getLength());
			charCounterInLine = searchResult.length();
			currentPosInLine = charCounterInLine;
			searchHandler.deactivateSearch();
		}
	}
		
	private void goToLastPos() {
		this.moveTo(this.getLength());
		deselect();
		currentPosInLine = charCounterInLine;
	}
	
	public void reset() {
		this.replaceText(header + '\n' + prompt.get());
	}
		
	protected void handleEnter() {
		charCounterInLine = 0;
		currentPosInLine = 0;
		String currentLine;
		if (searchHandler.isActive()) {
			currentLine = searchHandler.getCurrentSearchResult();
		} else {
			currentLine = this.getInput();
		}
		boolean endsWithNewline = currentLine.endsWith("\\");
		if(endsWithNewline) {
			currentLine = currentLine.substring(0, currentLine.length() - 1);
		}
		if (!instructions.isEmpty() && instructions.get(instructions.size() - 1).getOption() != ConsoleInstructionOption.ENTER) {
			instructions.set(instructions.size() - 1, new ConsoleInstruction(currentLine, ConsoleInstructionOption.ENTER));
		} else {
			instructions.add(new ConsoleInstruction(currentLine, ConsoleInstructionOption.ENTER));
		}
		if(endsWithNewline) {
			instructionLengthInLine++;
			this.appendText("\n" + EMPTY_PROMPT);
			return;
		}
			
		for(int i = 0; i < instructionLengthInLine; i++) {
			ConsoleInstruction instruction = instructions.get(instructions.size() - instructionLengthInLine + i);
			interpreter.exec(instruction);
		}
			
		posInList = instructions.size() - 1;
		ConsoleInstruction instruction = instructions.get(posInList);
		ConsoleExecResult execResult = interpreter.exec(instruction);
		int from = this.getLength();
		if (execResult.getResultType() == ConsoleExecResultType.CLEAR) {
			reset();
			return;
		}
		this.appendText("\n" + execResult);
		if (execResult.getResultType() == ConsoleExecResultType.ERROR) {
			this.setStyle(from, from + execResult.toString().length() + 1, Collections.singletonList("error"));
		}
		instructionLengthInLine = 1;
		searchHandler.handleEnter();
		this.appendText('\n' + prompt.get());
		this.setStyle(getLineNumber(), Collections.emptyList());
		this.scrollYToPixel(Double.MAX_VALUE);
		goToLastPos();
	}
		
	private void handleDown() {
		deactivateSearch();
		if (instructions.isEmpty() || posInList == instructions.size() - 1) {
			return;
		}
		posInList = Math.min(posInList+1, instructions.size() - 1);
		setTextAfterArrowKey();
	}
	
	private void handleUp() {
		deactivateSearch();
		if (instructions.isEmpty() || posInList == -1) {
			return;
		}
		if (posInList == instructions.size() - 1) {
			String lastinstruction = instructions.get(instructions.size()-1).getInstruction();
			if (!lastinstruction.equals(this.getInput())) {
				if (instructions.get(posInList).getOption() == ConsoleInstructionOption.UP) {
					instructions.set(instructions.size() - 1, new ConsoleInstruction(this.getInput(), ConsoleInstructionOption.UP));
				} else {
					instructions.add(new ConsoleInstruction(this.getInput(), ConsoleInstructionOption.UP));
					setTextAfterArrowKey();
					return;
				}
			}
		}
		posInList = Math.max(posInList - 1, 0);
		setTextAfterArrowKey();
	}
		
	private void handleLeft() {
		deactivateSearch();
		if (currentPosInLine > 0 && this.getLength() - this.getCaretPosition() <= charCounterInLine) {
			currentPosInLine--;
			this.moveTo(this.getCaretPosition() - 1);
		} else if (currentPosInLine == 0) {
			super.deselect();
		}
	}
	
	private void handleRight() {
		deactivateSearch();
		if (currentPosInLine < charCounterInLine && this.getLength() - this.getCaretPosition() <= charCounterInLine) {
			currentPosInLine++;
			this.moveTo(this.getCaretPosition() + 1);
		}
	}
	
	
	private void setTextAfterArrowKey() {
		String currentLine = instructions.get(posInList).getInstruction();
		this.deleteText(this.getInputStart(), this.getLength());
		this.appendText(currentLine);
		charCounterInLine = currentLine.length();
		currentPosInLine = charCounterInLine;
		this.scrollYToPixel(Double.MAX_VALUE);
	}
	
	private void handleDeletion(KeyEvent e) {
		int maxPosInLine = charCounterInLine;
		if (searchHandler.handleDeletion(e)) {
			return;
		}
		if (searchHandler.isActive()) {
			maxPosInLine = charCounterInLine + 2 + searchHandler.getCurrentSearchResult().length();
		}
		if (!this.getSelectedText().isEmpty() || this.getLength() - this.getCaretPosition() > maxPosInLine) {
			return;
		}
		if (e.getCode().equals(KeyCode.BACK_SPACE)) {
			handleBackspace();
		} else {
			handleDelete();
		}
	}
	
	private void handleBackspace() {
		if (currentPosInLine > 0) {
			currentPosInLine = Math.max(currentPosInLine - 1, 0);
			charCounterInLine = Math.max(charCounterInLine - 1, 0);
			this.deletePreviousChar();
		}
	}
	
	private void handleDelete() {
		if (currentPosInLine < charCounterInLine) {
			charCounterInLine = Math.max(charCounterInLine - 1, 0);
			this.deleteNextChar();
		}
	}
	
	public StringProperty promptProperty() {
		return this.prompt;
	}
	
	public String getPrompt() {
		return this.promptProperty().get();
	}
	
	public void setPrompt(final String prompt) {
		this.promptProperty().set(prompt);
	}
	
	public int getLineNumber() {
		return this.getParagraphs().size()-1;
	}
	
	public int getLineStart() {
		return this.getAbsolutePosition(getLineNumber(), 0);
	}
	
	public String getLine() {
		return this.getParagraph(this.getLineNumber()).getText();
	}
	
	public int getInputStart() {
		int length = instructionLengthInLine > 1 ? EMPTY_PROMPT.length() : this.getPrompt().length();
		return this.getLineStart() + length;
	}
	
	public String getInput() {
		int length = instructionLengthInLine > 1 ? EMPTY_PROMPT.length() : this.getPrompt().length();
		return this.getLine().substring(length);
	}
	
	public List<String> saveInstructions() {
		return instructions.stream().map(ConsoleInstruction::getInstruction).collect(Collectors.toList());
	}
	
	public void loadInstructions(List<String> instructions) {
		this.instructions.clear();
		for (final String instruction : instructions) {
			this.instructions.add(new ConsoleInstruction(instruction, ConsoleInstructionOption.ENTER));
		}
		posInList = this.instructions.size();
	}
	
	public int getCurrentPosInLine() {
		return currentPosInLine;
	}
	
	public List<ConsoleInstruction> getInstructions() {
		return instructions;
	}
}
