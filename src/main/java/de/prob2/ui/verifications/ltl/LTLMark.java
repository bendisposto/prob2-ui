package de.prob2.ui.verifications.ltl;

import org.antlr.v4.runtime.Token;

public class LTLMark {

	private int line;
	private int pos;
	private int length;

	public LTLMark(int line, int pos, int length) {
		this.line = line;
		this.pos = pos;
		this.length = length;
	}

	public LTLMark(Token token, int length) {
		this(token.getLine(), token.getCharPositionInLine(), length);
	}

	public int getLine() {
		return line;
	}

	public void setLine(int line) {
		this.line = line;
	}

	public int getPos() {
		return pos;
	}

	public void setPos(int pos) {
		this.pos = pos;
	}

	public int getLength() {
		return length;
	}

	public void setLength(int length) {
		this.length = length;
	}

}