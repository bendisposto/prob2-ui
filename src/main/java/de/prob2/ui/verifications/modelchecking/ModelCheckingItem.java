package de.prob2.ui.verifications.modelchecking;

import java.util.Objects;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;

import de.prob.check.ModelCheckingOptions;
import de.prob.check.ModelCheckingOptions.Options;
import de.prob2.ui.verifications.Checked;
import de.prob2.ui.verifications.IExecutableItem;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.paint.Color;

public class ModelCheckingItem implements IExecutableItem {

	private ModelCheckingOptions options;
	
	private transient ModelCheckStats stats;
	
	private transient FontAwesomeIconView status;
	
	private transient FontAwesomeIconView deadlocks;
	
	private transient FontAwesomeIconView invariantViolations;
	
	private transient FontAwesomeIconView assertionViolations;
	
	private transient FontAwesomeIconView goals;
	
	private transient FontAwesomeIconView stopWhenAllOperationsCovered;
	
	private Checked checked;
	
	private String strategy;
	
	private BooleanProperty shouldExecute;

	public ModelCheckingItem(ModelCheckingOptions options, ModelCheckStats stats, String strategy) {
		Objects.requireNonNull(options);
		Objects.requireNonNull(stats);
		
		this.options = options;
		this.stats = stats;
		this.strategy = strategy;
		this.shouldExecute = new SimpleBooleanProperty(true);
		
		initialize();
	}
	
	public void setOptions(ModelCheckingOptions options) {
		this.options = options;
		initializeOptionIcons(options);
	}

	public ModelCheckingOptions getOptions() {
		return this.options;
	}
	
	public void setStats(ModelCheckStats stats) {
		this.stats = stats;
	}

	public ModelCheckStats getStats() {
		return this.stats;
	}
	
	public void setStrategy(String strategy) {
		this.strategy = strategy;
	}
	
	public String getStrategy() {
		return strategy;
	}
	
	public void setSelected(boolean selected) {
		this.shouldExecute.set(selected);
	}
	
	@Override
	public boolean selected() {
		return shouldExecute.get();
	}
	
	public BooleanProperty selectedProperty() {
		return shouldExecute;
	}
	
	public void initialize() {
		initializeStatus();
		initializeOptionIcons(options);
	}
	
	private void initializeStatus() {
		this.status = new FontAwesomeIconView(FontAwesomeIcon.QUESTION_CIRCLE);
		this.status.setFill(Color.BLUE);
		this.checked = Checked.NOT_CHECKED;
		this.stats = null;
	}
	
	private void initializeOptionIcons(ModelCheckingOptions options) {
		this.deadlocks = new FontAwesomeIconView(FontAwesomeIcon.QUESTION_CIRCLE);
		this.invariantViolations = new FontAwesomeIconView(FontAwesomeIcon.QUESTION_CIRCLE);
		this.assertionViolations = new FontAwesomeIconView(FontAwesomeIcon.QUESTION_CIRCLE);
		this.goals = new FontAwesomeIconView(FontAwesomeIcon.QUESTION_CIRCLE);
		this.stopWhenAllOperationsCovered = new FontAwesomeIconView(FontAwesomeIcon.QUESTION_CIRCLE);
		
		this.deadlocks.setFill(Color.BLUE);
		this.invariantViolations.setFill(Color.BLUE);
		this.assertionViolations.setFill(Color.BLUE);
		this.goals.setFill(Color.BLUE);
		this.stopWhenAllOperationsCovered.setFill(Color.BLUE);
		
		initializeOptionIcon(this.deadlocks, options, Options.FIND_DEADLOCKS);
		initializeOptionIcon(this.invariantViolations, options, Options.FIND_INVARIANT_VIOLATIONS);
		initializeOptionIcon(this.assertionViolations, options, Options.FIND_ASSERTION_VIOLATIONS);
		initializeOptionIcon(this.goals, options, Options.FIND_GOAL);
		initializeOptionIcon(this.stopWhenAllOperationsCovered, options, Options.STOP_AT_FULL_COVERAGE);
	}
	
	private void initializeOptionIcon(FontAwesomeIconView icon, ModelCheckingOptions options, Options option) {
		if(options.getPrologOptions().contains(option)) {
			icon.setIcon(FontAwesomeIcon.CHECK);
			icon.setFill(Color.GREEN);
		} else {
			icon.setIcon(FontAwesomeIcon.REMOVE);
			icon.setFill(Color.RED);
		}
	}
	
	public FontAwesomeIconView getStatus() {
		return status;
	}
	
	
	public void setCheckedSuccessful() {
		FontAwesomeIconView icon = new FontAwesomeIconView(FontAwesomeIcon.CHECK);
		icon.setFill(Color.GREEN);
		this.status = icon;
	}

	public void setCheckedFailed() {
		FontAwesomeIconView icon = new FontAwesomeIconView(FontAwesomeIcon.REMOVE);
		icon.setFill(Color.RED);
		this.status = icon;
	}
	
	public void setTimeout() {
		FontAwesomeIconView icon = new FontAwesomeIconView(FontAwesomeIcon.EXCLAMATION_TRIANGLE);
		icon.setFill(Color.YELLOW);
		this.status = icon;
	}

	
	public void setChecked(Checked checked) {
		this.checked = checked;
	}
	
	@Override
	public Checked getChecked() {
		return checked;
	}
	
	public FontAwesomeIconView getDeadlocks() {
		return deadlocks;
	}
	
	public FontAwesomeIconView getInvariantViolations() {
		return invariantViolations;
	}
	
	public FontAwesomeIconView getAssertionViolations() {
		return assertionViolations;
	}
	
	public FontAwesomeIconView getGoals() {
		return goals;
	}
	
	public FontAwesomeIconView getStopWhenAllOperationsCovered() {
		return stopWhenAllOperationsCovered;
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj == this) {
			return true;
		}
		if(!(obj instanceof ModelCheckingItem)) {
			return false;
		}
		ModelCheckingItem other = (ModelCheckingItem) obj;
		return options.equals(other.options);
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(options);
	}
	
	@Override
	public String toString() {
		return options.toString();
	}
	
}
