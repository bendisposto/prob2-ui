package de.prob2.ui.states;

import java.util.Objects;

import de.prob.animator.prologast.PrologASTNode;

public class StateItem<T extends PrologASTNode> {
	private final T contents;
	private final boolean errored;
	
	public StateItem(final T contents, final boolean errored) {
		super();
		
		Objects.requireNonNull(contents);
		
		this.contents = contents;
		this.errored = errored;
	}
	
	public T getContents() {
		return this.contents;
	}
	
	public boolean isErrored() {
		return this.errored;
	}
	
	@Override
	public String toString() {
		return String.format("%s{contents=%s, errored=%s}", this.getClass().getName(), this.getContents(), this.isErrored());
	}
}
