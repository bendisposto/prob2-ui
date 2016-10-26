package de.prob2.ui.consoles.b;

import de.prob2.ui.consoles.Console;
import javafx.scene.input.KeyEvent;

public class BConsole extends Console {
	
	private BInterpreter interpreter;

	public BConsole() {
		super();
		this.appendText("Prob 2.0 B Console \n >");
	}
	
	public void setInterpreter(BInterpreter interpreter) {
		this.interpreter = interpreter;
	}

	@Override
	protected void handleEnter(KeyEvent e) {
		super.handleEnterAbstract(e);
		if(!getCurrentLine().isEmpty()) {
			this.appendText("\n" + interpreter.exec(instructions.get(posInList)));
		}
		this.appendText("\n >");
	}
	
}
