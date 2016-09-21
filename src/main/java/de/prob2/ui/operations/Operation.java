package de.prob2.ui.operations;

import java.util.List;

public class Operation {
	public final String name;
	public final List<String> params;
	public final String id;
	public final String enablement;
	final boolean explored;
	final boolean errored;

	public Operation(final String id, final String name, final List<String> params, final boolean isEnabled,
			final boolean hasTimeout, final boolean explored, final boolean errored) {
		this.id = id;
		this.name = name;
		this.params = params;
		enablement = isEnabled ? "enabled" : hasTimeout ? "timeout" : "notEnabled";
		this.explored = explored;
		this.errored = errored;
	}

	@Override
	public String toString() {
		return String.format("%s(%s)", name, String.join(", ", params));
	}
	
	public boolean isEnabled() {
		return "enabled".equals(enablement);
	}
}
