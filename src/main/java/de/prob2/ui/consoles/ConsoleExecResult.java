package de.prob2.ui.consoles;

public class ConsoleExecResult {
	private final String consoleOutput;
	private final String result;
	
	public ConsoleExecResult(String consoleOutput, String result) {
		this.consoleOutput = consoleOutput;
		this.result = result;
	}
	
	public String getConsoleOutput() {
		return this.consoleOutput;
	}
	
	public String getResult() {
		return this.result;
	}
	
	@Override
	public String toString() {
		return consoleOutput + result;
	}
}