package de.prob2.ui.verifications.ltl.formula;

import com.google.inject.Injector;
import com.google.inject.Singleton;
import de.be4.classicalb.core.parser.ClassicalBParser;
import de.be4.ltl.core.parser.LtlParseException;
import de.prob.animator.command.EvaluationCommand;
import de.prob.animator.domainobjects.AbstractEvalResult;
import de.prob.animator.domainobjects.ErrorItem;
import de.prob.animator.domainobjects.LTL;
import de.prob.check.LTLError;
import de.prob.exception.ProBError;
import de.prob.ltl.parser.LtlParser;
import de.prob.statespace.State;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.stats.StatsView;
import de.prob2.ui.statusbar.StatusBar;
import de.prob2.ui.verifications.Checked;
import de.prob2.ui.verifications.CheckingType;
import de.prob2.ui.verifications.MachineStatusHandler;
import de.prob2.ui.verifications.ltl.ILTLItemHandler;
import de.prob2.ui.verifications.ltl.LTLMarker;
import de.prob2.ui.verifications.ltl.LTLParseListener;
import de.prob2.ui.verifications.ltl.LTLResultHandler;
import javafx.application.Platform;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@FXMLInjected
@Singleton
public class LTLFormulaChecker implements ILTLItemHandler {
	
	private static final Logger logger = LoggerFactory.getLogger(LTLFormulaChecker.class);
				
	private final CurrentTrace currentTrace;
	
	private final CurrentProject currentProject;
	
	private final ListProperty<Thread> currentJobThreads;
	
	private final LTLResultHandler resultHandler;
	
	private final Injector injector;
	
	@Inject
	private LTLFormulaChecker(final CurrentTrace currentTrace, final CurrentProject currentProject,
			final LTLResultHandler resultHandler, final Injector injector) {
		this.currentTrace = currentTrace;
		this.currentProject = currentProject;
		this.resultHandler = resultHandler;
		this.currentJobThreads = new SimpleListProperty<>(this, "currentJobThreads", FXCollections.observableArrayList());
		this.injector = injector;
	}
	
	public void checkMachine(Machine machine) {
		final ArrayList<Boolean> failed = new ArrayList<>();
		failed.add(false);
		for (LTLFormulaItem item : machine.getLTLFormulas()) {
			Checked result = this.checkFormula(item, machine);
			if(result == Checked.FAIL) {
				failed.set(0, true);
				machine.setLtlStatus(Machine.CheckingStatus.FAILED);
			}
			item.setChecked(result);
			if(Thread.currentThread().isInterrupted()) {
				return;
			}
		}
		Platform.runLater(() -> injector.getInstance(StatusBar.class).setLtlStatus(failed.get(0) ? StatusBar.CheckingStatus.ERROR : StatusBar.CheckingStatus.SUCCESSFUL));
	
	}
	
	public void checkMachine() {
		Machine machine = currentProject.getCurrentMachine();
		Thread checkingThread = new Thread(() -> {
			checkMachine(machine);
			Platform.runLater(() -> injector.getInstance(MachineStatusHandler.class).updateMachineStatus(machine, CheckingType.LTL));
			currentJobThreads.remove(Thread.currentThread());
		}, "LTL Checking Thread");
		currentJobThreads.add(checkingThread);
		checkingThread.start();
	}
	
	public Checked checkFormula(LTLFormulaItem item, Machine machine) {
		if(!item.selected()) {
			return Checked.NOT_CHECKED;
		}
		State stateid = currentTrace.getCurrentState();
		LtlParser parser = new LtlParser(item.getCode());
		parser.setPatternManager(machine.getPatternManager());
		List<LTLMarker> errorMarkers = new ArrayList<>();
		Object result = getResult(parser, errorMarkers, item);
		return resultHandler.handleFormulaResult(item, errorMarkers, result, stateid);
	}
	
	public void checkFormula(LTLFormulaItem item) {
		Machine machine = currentProject.getCurrentMachine();
		Thread checkingThread = new Thread(() -> {
			Checked result = checkFormula(item, machine);
			item.setChecked(result);
			Platform.runLater(() -> injector.getInstance(MachineStatusHandler.class).updateMachineStatus(machine, CheckingType.LTL));
			if(item.getCounterExample() != null) {
				currentTrace.set(item.getCounterExample());
			}
			currentJobThreads.remove(Thread.currentThread());
		}, "LTL Checking Thread");
		currentJobThreads.add(checkingThread);
		checkingThread.start();
	}
	
	public void checkFormula(LTLFormulaItem item, LTLFormulaStage formulaStage) {
		Machine machine = currentProject.getCurrentMachine();
		Checked result = checkFormula(item, machine);
		item.setChecked(result);
		Thread checkingThread = new Thread(() -> {
			Platform.runLater(() -> {
				if(item.getChecked() == Checked.PARSE_ERROR) {
					formulaStage.setErrors(item.getResultItem().getMessage());
					return;
				}
				formulaStage.close();
				injector.getInstance(MachineStatusHandler.class).updateMachineStatus(machine, CheckingType.LTL);
			});
			if(item.getCounterExample() != null) {
				currentTrace.set(item.getCounterExample());
			}
			currentJobThreads.remove(Thread.currentThread());
		}, "LTL Checking Thread");
		currentJobThreads.add(checkingThread);
		checkingThread.start();
	}
	
	private Object getResult(LtlParser parser, List<LTLMarker> errorMarkers, LTLFormulaItem item) {
		State stateid = currentTrace.getCurrentState();
		LTLParseListener parseListener = parseFormula(parser);
		errorMarkers.addAll(parseListener.getErrorMarkers());
		EvaluationCommand lcc = null;
		LTL formula = null;
		try {
			if(!parseListener.getErrorMarkers().isEmpty()) {
				formula = new LTL(item.getCode(), new ClassicalBParser());
			} else {
				formula = new LTL(item.getCode(), new ClassicalBParser(), parser);
			}
			lcc = formula.getCommand(stateid);
			currentTrace.getStateSpace().execute(lcc);
			AbstractEvalResult res = lcc.getValue();
			if(res instanceof LTLError) {
				//TODO
				//LTLError error = (LTLError) res;
				//errorMarkers.add(new LTLMarker("error", res.getTokenLine(), parseError.getTokenColumn(), parseError.getMessage().length(), error.getMessage()));
			}
			injector.getInstance(StatsView.class).update(currentTrace.get());
		} catch (ProBError error) {
			logger.error("Could not parse LTL formula: ", error);
			ProBError parseError = error;
			List<ErrorItem.Location> errorLocations = parseError.getErrors().stream()
					.flatMap(err -> err.getLocations().stream())
					.collect(Collectors.toList());
			errorMarkers.addAll(errorLocations
				.stream()
				.map(location -> new LTLMarker("error", location.getStartLine(), location.getStartColumn(), location.getEndColumn() - location.getStartColumn(), error.getMessage()))
				.collect(Collectors.toList()));
			return error;
		} catch (LtlParseException error) {
			logger.error("Could not parse LTL formula: ", error);
			LtlParseException parseError = error;
			errorMarkers.add(new LTLMarker("error", parseError.getTokenLine(), parseError.getTokenColumn(), parseError.getMessage().length(), parseError.getMessage()));
			return error;
		}
		return lcc;
	}
	
	private LTLParseListener parseFormula(LtlParser parser) {
		LTLParseListener parseListener = new LTLParseListener();
		parser.removeErrorListeners();
		parser.addErrorListener(parseListener);
		parser.addWarningListener(parseListener);
		parser.parse();
		return parseListener;
	}
	
	public ListProperty<Thread> currentJobThreadsProperty() {
		return currentJobThreads;
	}
		
}
