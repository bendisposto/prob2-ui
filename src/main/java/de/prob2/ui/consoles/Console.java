package de.prob2.ui.consoles;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

public abstract class Console extends TextArea {
	private static final Set<KeyCode> REST = EnumSet.of(KeyCode.ESCAPE, KeyCode.SCROLL_LOCK, KeyCode.PAUSE, KeyCode.NUM_LOCK, KeyCode.INSERT, KeyCode.CONTEXT_MENU, KeyCode.CAPS);
	
	protected List<ConsoleInstruction> instructions;
	protected int charCounterInLine = 0;
	protected int currentPosInLine = 0;
	protected int posInList = -1;
	protected boolean backwardSearchActive = false;
	protected String backwardSearchCurrent ="";

	public Console() {
		this.setContextMenu(new ContextMenu());
		this.instructions = new ArrayList<>();
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
			this.setText(oldText);
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
		
		
	@Override
	public void forward() {
		if(currentPosInLine <= charCounterInLine && this.getLength() - this.getCaretPosition() <= charCounterInLine) {		
			super.forward();
			currentPosInLine = charCounterInLine - (this.getLength() - this.getCaretPosition());
			this.setScrollTop(Double.MIN_VALUE);
		}
	}
	
	@Override
	public void backward() {
		//handleLeft
		if(currentPosInLine > 0 && this.getLength() - this.getCaretPosition() <= charCounterInLine) {
			super.backward();
			currentPosInLine = charCounterInLine - (this.getLength() - this.getCaretPosition());
			this.setScrollTop(Double.MIN_VALUE);
		} else if(currentPosInLine == 0) {
			super.deselect();
		}
	}
	
	@Override
	public void selectForward() {
		if(currentPosInLine != charCounterInLine) {
			super.selectForward();
			currentPosInLine++;
		}
	}
	
	@Override
	public void selectBackward() {
		if(currentPosInLine != 0) {
			super.selectBackward();
			currentPosInLine--;
		}
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
				if(e.getCode() == KeyCode.A) {
					this.positionCaret(this.getCaretPosition() - currentPosInLine);
					currentPosInLine = 0;
				} else if(e.getCode() == KeyCode.E) {
					this.positionCaret(this.getLength());
					currentPosInLine = charCounterInLine;
				} else if(e.getCode() == KeyCode.R) {
					if(backwardSearchActive == false) {
						backwardSearchActivate();
					}
				}
				e.consume();
			}
		});
		
		this.setOnKeyPressed(e -> {
			if (e.getCode() == KeyCode.UP || e.getCode() == KeyCode.DOWN) {
				handleArrowKeys(e);
				this.setScrollTop(Double.MAX_VALUE);
			} else if (e.getCode().isNavigationKey()) {
				if(e.getCode() != KeyCode.LEFT && e.getCode() != KeyCode.RIGHT) {
					e.consume();
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
		});
	}
	
	protected void backwardSearchActivate() {
		int posOfEnter = Math.max(this.getText().lastIndexOf("\n"), this.getText().length() - 1);
		this.setText(this.getText().substring(0, posOfEnter) + "(backward search active): '': " + getCurrentLine());
		this.positionCaret(this.getText().length());
		backwardSearchActive = true;
	}
	//backwardSearch stays active Insert char, Backspace, Delete, Rest
	
	protected String backwardSearchResult() {
		String result = "";
		for(int i = instructions.size() - 1; i >= 0; i--) {
			if(instructions.get(i).getInstruction().contains(backwardSearchCurrent)) {
				return result;
			}
		}
		return null;
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

	}
	
	private void goToLastPos() {
		this.positionCaret(this.getLength());
		currentPosInLine = charCounterInLine;
	}
	
	protected abstract void handleEnter(KeyEvent e);
	
	protected void handleEnterAbstract(KeyEvent e) {
		charCounterInLine = 0;
		currentPosInLine = 0;
		e.consume();
		if(!getCurrentLine().isEmpty()) {
			if(!instructions.isEmpty() && instructions.get(instructions.size() - 1).getOption() != ConsoleInstructionOption.ENTER) {
				instructions.set(instructions.size() - 1, new ConsoleInstruction(getCurrentLine(), ConsoleInstructionOption.ENTER));
			} else {
				instructions.add(new ConsoleInstruction(getCurrentLine(), ConsoleInstructionOption.ENTER));
			}
			posInList = instructions.size() - 1;
		}
	}
	
	private void handleArrowKeys(KeyEvent e) {
		boolean needReturn;
		if(e.getCode().equals(KeyCode.UP)) {
			needReturn = handleUp(e);
		} else {
			needReturn = handleDown(e);				
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
	
	
	private void setTextAfterArrowKey() {
		int posOfEnter = this.getText().lastIndexOf("\n");
		this.setText(this.getText().substring(0, posOfEnter + 3));
		String currentLine = instructions.get(posInList).getInstruction();
		charCounterInLine = currentLine.length();
		currentPosInLine = charCounterInLine;
		this.appendText(currentLine);
	}
	
	
	private void handleRest(KeyEvent e) {
		if(REST.contains(e.getCode())) {
			e.consume();
		}
	}
	
	private void handleDeletion(KeyEvent e) {
		boolean needReturn;
		if(!this.getSelectedText().isEmpty() || this.getLength() - this.getCaretPosition() > charCounterInLine || e.isShortcutDown() || e.isAltDown()) {
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
		if(backwardSearchActive) {
			return backwardSearchCurrent;
		}
		int posOfEnter = this.getText().lastIndexOf("\n");
		return this.getText().substring(posOfEnter + 3, this.getText().length());
	}
	
	public int getCurrentPosInLine() {
		return currentPosInLine;
	}
	
}