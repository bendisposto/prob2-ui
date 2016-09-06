package de.prob2.ui.preferences;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;

import com.google.inject.Inject;

import de.be4.classicalb.core.parser.exceptions.BException;
import de.prob.animator.command.GetCurrentPreferencesCommand;
import de.prob.animator.command.GetDefaultPreferencesCommand;
import de.prob.animator.domainobjects.ProBPreference;
import de.prob.scripting.Api;
import de.prob.statespace.AnimationSelector;
import de.prob.statespace.StateSpace;
import de.prob.statespace.Trace;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableMap;

public final class ProBPreferences {
	private final AnimationSelector animationSelector;
	private final Api api;
	private StateSpace stateSpace;
	private final ObservableMap<String, ProBPreference> cachedPreferences;
	private final ObservableMap<String, String> cachedPreferenceValues;
	private final ObservableMap<String, String> changedPreferences;
	private final BooleanProperty changesApplied;
	
	@Inject
	private ProBPreferences(
		final AnimationSelector animationSelector,
		final Api api
	) {
		this.animationSelector = animationSelector;
		this.api = api;
		this.cachedPreferences = FXCollections.observableHashMap();
		this.cachedPreferenceValues = FXCollections.observableHashMap();
		this.changedPreferences = FXCollections.observableHashMap();
		this.changesApplied = new SimpleBooleanProperty(true);
		this.changedPreferences.addListener((MapChangeListener<? super String, ? super String>)change -> {
			this.changesApplied.set(change.getMap().isEmpty());
		});
		this.stateSpace = null;
	}
	
	/**
	 * A property indicating whether all changes have been applied.
	 * 
	 * @return a property indicating whether all changes have been applied
	 */
	public ReadOnlyBooleanProperty changesAppliedProperty() {
		return this.changesApplied;
	}
	
	/**
	 * Get whether all changes have been applied.
	 * 
	 * @return whether all changes have been applied
	 */
	public boolean getChangesApplied() {
		return this.changesApplied.get();
	}
	
	/**
	 * Ensure that {@link #stateSpace} is not {@code null}.
	 * 
	 * @throws IllegalStateException if {@link #stateSpace} is {@code null}
	 */
	private void checkStateSpace() {
		if (!this.hasStateSpace()) {
			throw new IllegalStateException("Cannot use ProBPreferences without setting a StateSpace first");
		}
	}
	
	/**
	 * Get the {@link StateSpace} currently used by this instance.
	 * If this method returns {@code null}, this instance has no {@link StateSpace}, and most methods will throw an {@link IllegalStateException} when called.
	 * 
	 * @return the {@link StateSpace} currently used by this instance
	 */
	public StateSpace getStateSpace() {
		return this.stateSpace;
	}
	
	/**
	 * Return whether this instance has a {@link StateSpace}. This is equivalent to {@code this.getStateSpace() != null}.
	 * 
	 * @return whether this instance has a {@link StateSpace}
	 */
	public boolean hasStateSpace() {
		return this.stateSpace != null;
	}
	
	/**
	 * Set a {@link StateSpace} to be used by this instance.
	 * This method must be called with a non-null {@code stateSpace} before most of the other methods can be used, and will throw an {@link IllegalStateException} otherwise.
	 * 
	 * @param stateSpace the {@link StateSpace} to use
	 */
	public void setStateSpace(final StateSpace stateSpace) {
		this.stateSpace = stateSpace;
		this.changedPreferences.clear();
		if (this.stateSpace == null) {
			this.cachedPreferences.clear();
			this.cachedPreferenceValues.clear();
		} else {
			final GetDefaultPreferencesCommand cmd1 = new GetDefaultPreferencesCommand();
			this.stateSpace.execute(cmd1);
			for (ProBPreference pref : cmd1.getPreferences()) {
				this.cachedPreferences.put(pref.name, pref);
			}
			
			final GetCurrentPreferencesCommand cmd2 = new GetCurrentPreferencesCommand();
			this.stateSpace.execute(cmd2);
			this.cachedPreferenceValues.putAll(cmd2.getPreferences());
		}
	}
	
	/**
	 * Get information about all available preferences.
	 * The returned {@link ProBPreference} objects do not include the current values of the preferences. To get these values, use {@link #getPreferenceValue(String)} or {@link #getPreferenceValues()}.
	 * 
	 * @return information about all available preferences
	 * 
	 * @see #getPreferenceValue(String)
	 * @see #getPreferenceValues()
	 */
	public Collection<ProBPreference> getPreferences() {
		this.checkStateSpace();
		
		return Collections.unmodifiableSet(new HashSet<>(this.cachedPreferences.values()));
	}
	
	/**
	 * Get the current value of the given preference.
	 * 
	 * @param name the preference to get the value for
	 * @return the preference's current value
	 * 
	 * @see #getPreferenceValues()
	 */
	public String getPreferenceValue(final String name) {
		Objects.requireNonNull(name);
		this.checkStateSpace();
		
		return this.changedPreferences.containsKey(name) ? this.changedPreferences.get(name) : this.cachedPreferenceValues.get(name);
	}
	
	/**
	 * Get the current values of all preferences.
	 * 
	 * @return the current values of all preferences
	 * 
	 * @see #getPreferenceValue(String)
	 * @see #getPreferences()
	 */
	public Map<String, String> getPreferenceValues() {
		this.checkStateSpace();
		
		final Map<String, String> prefs = new HashMap<>(this.cachedPreferenceValues);
		prefs.putAll(this.changedPreferences);
		return Collections.unmodifiableMap(prefs);
	}
	
	/**
	 * Set the value of the given preference.
	 * Note that for some preferences to take effect, the current model needs to be reloaded using {@link #apply()}.
	 * 
	 * @param name the preference to set
	 * @param value the value to set the preference to
	 * 
	 * @see #apply()
	 */
	public void setPreferenceValue(String name, String value) {
		Objects.requireNonNull(name);
		Objects.requireNonNull(value);
		this.checkStateSpace();
		
		this.changedPreferences.put(name, value);
		if (value.equals(this.cachedPreferenceValues.get(name))) {
			this.changedPreferences.remove(name);
		}
	}
	
	/**
	 * Get a read-only observable map containing all preferences and their values that were changed since the last {@link #apply()}.
	 *
	 * @return a read-only observable map containing all changed preferences and their values
	 */
	public ObservableMap<String, String> getChangedPreferences() {
		return FXCollections.unmodifiableObservableMap(this.changedPreferences);
	}
	
	/**
	 * Reload the current model and apply all preference changes.
	 *
	 * @throws BException when thrown by {@link Api#b_load(String, Map)}
	 * @throws IOException when thrown by {@link Api#b_load(String, Map)}
	 * @throws IllegalStateException if there is no current trace
	 * 
	 * @see #setPreferenceValue(String, String)
	 * @see #rollback()
	 */
	public void apply() throws BException, IOException {
		final Trace oldTrace = this.animationSelector.getCurrentTrace();
		if (oldTrace == null) {
			throw new IllegalStateException("Cannot apply preferences without a current trace");
		}
		final Map<String, String> newPrefs = this.getPreferenceValues();
		final String filename = oldTrace.getModel().getModelFile().getAbsolutePath();
		final StateSpace newSpace = api.b_load(filename, newPrefs);
		final Trace newTrace = new Trace(newSpace);
		this.animationSelector.removeTrace(oldTrace);
		this.animationSelector.addNewAnimation(newTrace);
	}
	
	/**
	 * Rollback all preference changes made since the last model reload.
	 * 
	 * @see #apply()
	 */
	public void rollback() {
		this.checkStateSpace();
		
		this.changedPreferences.clear();
	}
}