package de.prob2.ui.consoles;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.fxmisc.richtext.CodeArea;

import javafx.scene.control.ContextMenu;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

public abstract class Console extends CodeArea {
	private static final Set<KeyCode> REST = EnumSet.of(KeyCode.ESCAPE, KeyCode.SCROLL_LOCK, KeyCode.PAUSE, KeyCode.NUM_LOCK, KeyCode.INSERT, KeyCode.CONTEXT_MENU, KeyCode.CAPS);
    
	protected List<ConsoleInstruction> instructions;
	protected int charCounterInLine = 0;
	protected int currentPosInLine = 0;
	protected int posInList = -1;
	protected ConsoleSearchHandler searchHandler;

	public Console() {
		this.setContextMenu(new ContextMenu());
		this.instructions = new ArrayList<>();
		this.searchHandler = new ConsoleSearchHandler(this, instructions);
		setListeners();
	}
		
	@Override
	public void paste() {
		if(this.getLength() - 1 - this.getCaretPosition() >= charCounterInLine) {
			goToLastPos();
		}
		String oldText = this.getText();
		int posOfEnter = oldText.lastIndexOf('\n');
		super.paste();
		int diff = this.getLength() - oldText.length();
		String currentLine = this.getText().substring(posOfEnter + 3, this.getText().length());
		if(currentLine.contains("\n")) {
			this.replaceText(oldText);
			goToLastPos();
			return;
		}
		charCounterInLine += diff;
		currentPosInLine += diff;
	}
		
	@Override
	public void copy() {
		super.copy();
		goToLastPos();
	}
			
	protected void setListeners() {
		setMouseEvent();
		setKeyEvent();
	}
	
	private void setMouseEvent() {
		this.addEventFilter(MouseEvent.ANY, e -> {
			if(e.getButton() == MouseButton.PRIMARY && (this.getLength() - 1 - this.getCaretPosition() < charCounterInLine)) {
				currentPosInLine = charCounterInLine - (this.getLength() - this.getCaretPosition());
			}
		});
	}
	
	protected void setKeyEvent() {
		this.addEventFilter(KeyEvent.ANY, e -> {
			if(e.getCode() == KeyCode.Z && (e.isShortcutDown() || e.isAltDown())) {
				e.consume();
			}
			if(e.isControlDown()) {
				controlDown(e);
			}
		});
		
		this.addEventFilter(KeyEvent.KEY_PRESSED, e-> {
			if(e.isControlDown() && e.getCode() == KeyCode.R) {
				if(!searchHandler.isActive()) {
					activateSearch();
				} else {
					searchHandler.searchNext();
				}
			}
		});
		
		this.setOnKeyPressed(this::keyPressed);
	}

	private void keyPressed(KeyEvent e) {
		if (e.getCode() == KeyCode.UP || e.getCode() == KeyCode.DOWN) {
			handleArrowKeys(e);
			e.consume();
			//this.setScrollTop(Double.MAX_VALUE);
		} else if (e.getCode().isNavigationKey()) {
			if((e.getCode() != KeyCode.LEFT && e.getCode() != KeyCode.RIGHT) || e.isShiftDown()) {
				e.consume();
			} else {
				handleArrowKeys(e);
			}
		} else if (e.getCode() == KeyCode.BACK_SPACE || e.getCode() == KeyCode.DELETE) {
			handleDeletion(e);
		} else if (e.getCode() == KeyCode.ENTER) {
			handleEnter(e);
		} else if (!e.getCode().isFunctionKey() && !e.getCode().isMediaKey() && !e.getCode().isModifierKey()) {
			handleInsertChar(e);
		} else {
			handleRest(e);
		}
	}

	private void controlDown(KeyEvent e) {
		if(!searchHandler.isActive()) {
			if(e.getCode() == KeyCode.A) {
				this.positionCaret(this.getCaretPosition() - currentPosInLine);
				currentPosInLine = 0;
				e.consume();
			} else if(e.getCode() == KeyCode.E) {
				this.positionCaret(this.getLength());
				currentPosInLine = charCounterInLine;
			}
		} else if(e.getCode() == KeyCode.V || e.getCode() == KeyCode.A) {
			e.consume();
		}
	}

	protected void activateSearch() {
		int posOfEnter = this.getText().lastIndexOf("\n");
		this.replaceText(this.getText().substring(0, posOfEnter + 1) + ConsoleSearchHandler.FOUND + getCurrentLine());
		this.positionCaret(this.getText().lastIndexOf("'"));
		currentPosInLine = 0;
		charCounterInLine = 0;
		searchHandler.activateSearch();
	}
	
	protected void deactivateSearch() {
		int posOfEnter = this.getText().lastIndexOf("\n");
		String searchResult = searchHandler.getCurrentSearchResult();
		this.replaceText(this.getText().substring(0, posOfEnter + 1) + " >" + searchResult);
		this.positionCaret(this.getText().length());
		charCounterInLine = searchResult.length();
		currentPosInLine = charCounterInLine;
		searchHandler.deactivateSearch();
	}
	
	
	protected void handleInsertChar(KeyEvent e) {
		if(e.getText().isEmpty() || (!(e.isShortcutDown() || e.isAltDown()) && (this.getLength() - this.getCaretPosition()) > charCounterInLine)) {
			if(!(e.getCode() == KeyCode.UNDEFINED || e.getCode() == KeyCode.ALT_GRAPH)) {
				goToLastPos();
			}
			if(e.getText().isEmpty()) {
				e.consume();
				return;
			}
		}
		if (e.isShortcutDown() || e.isAltDown()) {
			return;
		}
		charCounterInLine++;
		currentPosInLine++;
		posInList = instructions.size() - 1;
		searchHandler.handleKey(e);
	}
	
	
	private void goToLastPos() {
		this.positionCaret(this.getLength());
		deselect();
		currentPosInLine = charCounterInLine;
	}
	
	protected abstract void handleEnter(KeyEvent e);
	
	protected void handleEnterAbstract(KeyEvent e) {
		charCounterInLine = 0;
		currentPosInLine = 0;
		e.consume();
		String instruction = getCurrentLine();
		if(searchHandler.isActive()) {
			instruction = searchHandler.getCurrentSearchResult();
		}
		if(!getCurrentLine().isEmpty()) {
			if(!instructions.isEmpty() && instructions.get(instructions.size() - 1).getOption() != ConsoleInstructionOption.ENTER) {
				instructions.set(instructions.size() - 1, new ConsoleInstruction(instruction, ConsoleInstructionOption.ENTER));
			} else {
				instructions.add(new ConsoleInstruction(instruction, ConsoleInstructionOption.ENTER));
			}
			posInList = instructions.size() - 1;
		}
		searchHandler.handleEnter();
	}
	
	private void handleArrowKeys(KeyEvent e) {
		boolean needReturn = false;
		if(searchHandler.isActive()) {
			deactivateSearch();
		}
		if(e.getCode().equals(KeyCode.UP)) {
			needReturn = handleUp(e);
		} else if(e.getCode().equals(KeyCode.DOWN)) {
			needReturn = handleDown(e);				
		} else if(e.getCode().equals(KeyCode.LEFT)) {
			handleLeft(e);
			needReturn = true;
		} else {
			handleRight();
			needReturn = true;
		}
		if(needReturn) {
			return;
		}
		setTextAfterArrowKey();
	}
	
	private boolean handleUp(KeyEvent e) {
		e.consume();
		if(posInList == -1) { 
			return true;
		}
		if(posInList == instructions.size() - 1) {
			String lastinstruction = instructions.get(instructions.size()-1).getInstruction();
			if(!lastinstruction.equals(getCurrentLine()) && posInList == instructions.size() - 1) {
				if(instructions.get(posInList).getOption() == ConsoleInstructionOption.UP) {
					instructions.set(instructions.size() - 1, new ConsoleInstruction(getCurrentLine(), ConsoleInstructionOption.UP));
				} else {
					instructions.add(new ConsoleInstruction(getCurrentLine(), ConsoleInstructionOption.UP));
					setTextAfterArrowKey();
					return true;
				}
			}
		}
		posInList = Math.max(posInList - 1, 0);
		return false;
	}
	
	private boolean handleDown(KeyEvent e) {
		e.consume();
		if(posInList == instructions.size() - 1) {
			return true;
		}
		posInList = Math.min(posInList+1, instructions.size() - 1);
		return false;
	}
	
	private void handleLeft(KeyEvent e) {
		if(currentPosInLine > 0 && this.getLength() - this.getCaretPosition() <= charCounterInLine) {
			currentPosInLine--;
		} else if(currentPosInLine == 0) {
			e.consume();
			super.deselect();
		}
		if(searchHandler.isActive()) {
			deactivateSearch();
		}
	}
	
	private void handleRight() {
		if(currentPosInLine < charCounterInLine && this.getLength() - this.getCaretPosition() <= charCounterInLine) {		
			currentPosInLine++;
		}
		if(searchHandler.isActive()) {
			deactivateSearch();
		}
	}
	
	
	private void setTextAfterArrowKey() {
		int posOfEnter = this.getText().lastIndexOf("\n");
		String currentLine = instructions.get(posInList).getInstruction();
		this.replaceText(this.getText().substring(0, posOfEnter + 3) + currentLine);
		charCounterInLine = currentLine.length();
		currentPosInLine = charCounterInLine;
	}
	
	
	private void handleRest(KeyEvent e) {
		if(REST.contains(e.getCode())) {
			e.consume();
		}
	}
	
	private void handleDeletion(KeyEvent e) {
		boolean needReturn;
		int maxPosInLine = charCounterInLine;
		if(searchHandler.handleDeletion(e)) {
			return;
		}
		if(searchHandler.isActive()) {
			maxPosInLine = charCounterInLine + 2 + searchHandler.getCurrentSearchResult().length();
		}
		if(!this.getSelectedText().isEmpty() || this.getLength() - this.getCaretPosition() > maxPosInLine || e.isShortcutDown() || e.isAltDown()) {
			e.consume();
			return;
		}
		if(e.getCode().equals(KeyCode.BACK_SPACE)) {
			needReturn = handleBackspace(e);
			if(needReturn) {
				return;
			}
		} else {
			needReturn = handleDelete(e);
			if(needReturn) {
				return;
			}
		}
	}
	
	private boolean handleBackspace(KeyEvent e) {
		if(currentPosInLine > 0) {
			currentPosInLine = Math.max(currentPosInLine - 1, 0);
			charCounterInLine = Math.max(charCounterInLine - 1, 0);	
		} else {
			e.consume();
			return true;
		}
		return false;
	}
	
	private boolean handleDelete(KeyEvent e) {
		if(currentPosInLine < charCounterInLine) {
			charCounterInLine = Math.max(charCounterInLine - 1, 0);
		} else if(currentPosInLine == charCounterInLine) {
			e.consume();
			return true;
		}
		return false;
	}
	
	public String getCurrentLine() {
		int posOfEnter = this.getText().lastIndexOf("\n");
		return this.getText().substring(posOfEnter + 3, this.getText().length());
	}
		
	public int getCurrentPosInLine() {
		return currentPosInLine;
	}
	
	public List<ConsoleInstruction> getInstructions() {
		return instructions;
	}
	
	public List<String> getInstructionEntries() {
		//last 100 Entries
		List<String> entries = new ArrayList<>();
		for(int i = Math.max(instructions.size() - 100,0); i < instructions.size(); i++) {
			entries.add(instructions.get(i).getInstruction());
		}
		return entries;
	}
	
	public void increaseCounter() {
		posInList++;
	}
	
}
