package de.prob2.ui.verifications.cbc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob.animator.command.ConstraintBasedAssertionCheckCommand;
import de.prob.animator.command.ConstraintBasedRefinementCheckCommand;
import de.prob.animator.command.FindStateCommand;
import de.prob.animator.command.GetRedundantInvariantsCommand;
import de.prob.check.CBCDeadlockFound;
import de.prob.check.CBCInvariantViolationFound;
import de.prob.check.CheckError;
import de.prob.check.ModelCheckOk;
import de.prob.check.NotYetFinished;
import de.prob.check.RefinementCheckCounterExample;
import de.prob.statespace.State;
import de.prob.statespace.StateSpace;
import de.prob.statespace.Trace;

import de.prob2.ui.internal.StageManager;
import de.prob2.ui.verifications.AbstractCheckableItem;
import de.prob2.ui.verifications.AbstractResultHandler;
import de.prob2.ui.verifications.Checked;
import de.prob2.ui.verifications.CheckingResultItem;
import de.prob2.ui.verifications.CheckingType;

import javafx.scene.control.Alert;

@Singleton
public class CBCResultHandler extends AbstractResultHandler {
	
	@Inject
	public CBCResultHandler(final StageManager stageManager, final ResourceBundle bundle) {
		super(stageManager, bundle);
		
		this.type = CheckingType.CBC;
		this.success.addAll(Arrays.asList(ModelCheckOk.class));
		this.counterExample.addAll(Arrays.asList(CBCInvariantViolationFound.class, CBCDeadlockFound.class,
												RefinementCheckCounterExample.class));
		this.error.addAll(Arrays.asList(CBCDeadlockFound.class, CheckError.class));
		this.exception.addAll(Arrays.asList(CBCParseError.class));
		this.interrupted.addAll(Arrays.asList(NotYetFinished.class));
	}
	
	public void showResult(CheckingResultItem resultItem, AbstractCheckableItem item, List<Trace> traces) {
		super.showResult(resultItem, item);
		((CBCFormulaItem) item).getCounterExamples().clear();
		for(Trace trace: traces) {
			((CBCFormulaItem) item).getCounterExamples().add(trace);
		}
	}
	
	public void handleFormulaResult(CBCFormulaItem item, Object result, State stateid) {
		Class<?> clazz = result.getClass();
		if(success.contains(clazz)) {
			handleItem(item, Checked.SUCCESS);
		} else if(error.contains(clazz) || counterExample.contains(clazz) || exception.contains(clazz)) {
			handleItem(item, Checked.FAIL);
		} else {
			handleItem(item, Checked.INTERRUPTED);
		}
		ArrayList<Trace> traces = new ArrayList<>();
		CheckingResultItem resultItem = handleFormulaResult(result, stateid, traces);
		this.showResult(resultItem, item, traces);
	}

	@Override
	protected List<Trace> handleCounterExample(Object result, State stateid) {
		if(result instanceof CBCInvariantViolationFound) {
			return handleInvariantCounterExamples(result, stateid);
		} else if(result instanceof CBCDeadlockFound) {
			return handleDeadlockCounterExample(result, stateid);
		}
		return handleRefinementCounterExample(result, stateid);
	}
	
	private List<Trace> handleInvariantCounterExamples(Object result, State stateid) {
		ArrayList<Trace> counterExamples = new ArrayList<>();
		CBCInvariantViolationFound violation = (CBCInvariantViolationFound) result;
		int size = violation.getCounterexamples().size();
		for(int i = 0; i < size; i++) {
			counterExamples.add(violation.getTrace(i, stateid.getStateSpace()));
		}
		return counterExamples;
	}
	
	private List<Trace> handleDeadlockCounterExample(Object result, State stateid) {
		ArrayList<Trace> counterExamples = new ArrayList<>();
		counterExamples.add(((CBCDeadlockFound) result).getTrace(stateid.getStateSpace()));
		return counterExamples;
	}
	
	private List<Trace> handleRefinementCounterExample(Object result, State stateid) {
		ArrayList<Trace> counterExamples = new ArrayList<>();
		counterExamples.add(((RefinementCheckCounterExample) result).getTrace(stateid.getStateSpace()));
		return counterExamples;
	}
	
	public void handleFindValidState(CBCFormulaItem item, FindStateCommand cmd, StateSpace stateSpace) {
		FindStateCommand.ResultType result = cmd.getResult();
		item.setExample(null);
		// noinspection IfCanBeSwitch // Do not replace with switch, because result can be null
		if (result == FindStateCommand.ResultType.STATE_FOUND) {
			showCheckingResult(item, bundle.getString("verifications.cbc.findValidState.result.found"), Checked.SUCCESS);
			item.setExample(cmd.getTrace(stateSpace));
		} else if (result == FindStateCommand.ResultType.NO_STATE_FOUND) {
			showCheckingResult(item, bundle.getString("verifications.cbc.findValidState.result.notFound"), Checked.FAIL);
		} else if (result == FindStateCommand.ResultType.INTERRUPTED) {
			showCheckingResult(item, bundle.getString("verifications.cbc.findValidState.result.interrupted"), Checked.INTERRUPTED);
		} else {
			showCheckingResult(item, bundle.getString("verifications.cbc.findValidState.result.error"), Checked.FAIL);
		}
	}
	
	public void handleFindRedundantInvariants(CBCFormulaItem item, GetRedundantInvariantsCommand cmd) {
		List<String> result = cmd.getRedundantInvariants();
		if (result.isEmpty()) {
			showCheckingResult(item, bundle.getString("verifications.cbc.findRedundantInvariants.result.notFound"), Checked.SUCCESS);
		} else {
			final String header = bundle.getString(cmd.isTimeout() ? "verifications.cbc.findRedundantInvariants.result.timeout" : "verifications.cbc.findRedundantInvariants.result.found");
			showCheckingResult(item, String.join("\n", result), header, Checked.FAIL);
		}
	}
	
	public void handleRefinementChecking(CBCFormulaItem item, ConstraintBasedRefinementCheckCommand cmd) {
		ConstraintBasedRefinementCheckCommand.ResultType result = cmd.getResult();
		String msg = cmd.getResultsString();
		if (result == null) {
			showCheckingResult(item, bundle.getString("verifications.cbc.refinementChecking.result.notARefinementMachine.message"), bundle.getString("verifications.cbc.refinementChecking.result.notARefinementMachine.header"), Checked.FAIL);
		} else if (result == ConstraintBasedRefinementCheckCommand.ResultType.NO_VIOLATION_FOUND) {
			showCheckingResult(item, msg, bundle.getString("verifications.cbc.refinementChecking.result.noViolationFound"), Checked.SUCCESS);
		} else if (result == ConstraintBasedRefinementCheckCommand.ResultType.VIOLATION_FOUND) {
			showCheckingResult(item, msg, bundle.getString("verifications.cbc.refinementChecking.result.violationFound"), Checked.FAIL);
		} else if (result == ConstraintBasedRefinementCheckCommand.ResultType.INTERRUPTED) {
			showCheckingResult(item, msg, bundle.getString("verifications.cbc.refinementChecking.result.interrupted"), Checked.INTERRUPTED);
		}
	}
	
	public void handleAssertionChecking(CBCFormulaItem item, ConstraintBasedAssertionCheckCommand cmd, StateSpace stateSpace) {
		ConstraintBasedAssertionCheckCommand.ResultType result = cmd.getResult();
		if (result == ConstraintBasedAssertionCheckCommand.ResultType.NO_COUNTER_EXAMPLE_EXISTS) {
			showCheckingResult(item, bundle.getString("verifications.cbc.assertionChecking.result.noCounterExampleExists"), Checked.SUCCESS);
		} else if (result == ConstraintBasedAssertionCheckCommand.ResultType.NO_COUNTER_EXAMPLE_FOUND) {
			showCheckingResult(item, bundle.getString("verifications.cbc.assertionChecking.result.noCounterExampleFound"), Checked.SUCCESS);
		} else if (result == ConstraintBasedAssertionCheckCommand.ResultType.COUNTER_EXAMPLE) {
			item.getCounterExamples().add(cmd.getTrace(stateSpace));
			showCheckingResult(item, bundle.getString("verifications.cbc.assertionChecking.result.counterExampleFound"), Checked.FAIL);
		} else if (result == ConstraintBasedAssertionCheckCommand.ResultType.INTERRUPTED) {
			showCheckingResult(item, bundle.getString("verifications.cbc.assertionChecking.result.interrupted"), Checked.INTERRUPTED);
		}
	}
		
	private void showCheckingResult(CBCFormulaItem item, String msg, String header, Checked checked) {
		Alert.AlertType alertType = checked == Checked.SUCCESS ? Alert.AlertType.INFORMATION : Alert.AlertType.ERROR;
		CheckingResultItem resultItem = new CheckingResultItem(alertType , checked, msg, header);
		super.showResult(resultItem, item);
		handleItem(item, checked);
	}
	
	private void showCheckingResult(CBCFormulaItem item, String msg, Checked checked) {
		showCheckingResult(item, msg, msg, checked);
	}
	

}
