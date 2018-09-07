package de.prob2.ui.animation.symbolic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob.animator.command.AbstractCommand;
import de.prob.animator.command.FindStateCommand;
import de.prob.animator.command.GetRedundantInvariantsCommand;
import de.prob.check.CBCDeadlockFound;
import de.prob.check.CheckError;
import de.prob.check.CheckInterrupted;
import de.prob.check.IModelCheckingResult;
import de.prob.check.ModelCheckOk;
import de.prob.check.NotYetFinished;
import de.prob.statespace.StateSpace;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.symbolic.ISymbolicResultHandler;
import de.prob2.ui.symbolic.SymbolicExecutionType;
import de.prob2.ui.symbolic.SymbolicFormulaItem;
import de.prob2.ui.verifications.AbstractCheckableItem;
import de.prob2.ui.verifications.AbstractResultHandler;
import de.prob2.ui.verifications.Checked;
import de.prob2.ui.verifications.CheckingResultItem;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.Region;

@Singleton
public class SymbolicAnimationResultHandler implements ISymbolicResultHandler {

	private static final String GENERAL_RESULT_MESSAGE = "verifications.result.message";
	
	private final ResourceBundle bundle;
	
	private final CurrentTrace currentTrace;
	
	protected ArrayList<Class<?>> success;
	protected ArrayList<Class<?>> error;
	protected ArrayList<Class<?>> interrupted;

	private final StageManager stageManager;
	
	@Inject
	public SymbolicAnimationResultHandler(final ResourceBundle bundle, final CurrentTrace currentTrace, final StageManager stageManager) {
		this.bundle = bundle;
		this.currentTrace = currentTrace;
		this.stageManager = stageManager;
		this.success = new ArrayList<>();
		this.error = new ArrayList<>();
		this.interrupted = new ArrayList<>();
		this.success.addAll(Arrays.asList(ModelCheckOk.class));
		this.error.addAll(Arrays.asList(CBCDeadlockFound.class, CheckError.class));
		this.interrupted.addAll(Arrays.asList(NotYetFinished.class, CheckInterrupted.class));
	}
	
	public void handleFindValidState(SymbolicAnimationFormulaItem item, FindStateCommand cmd, StateSpace stateSpace) {
		FindStateCommand.ResultType result = cmd.getResult();
		item.setExample(null);
		// noinspection IfCanBeSwitch // Do not replace with switch, because result can be null
		if (result == FindStateCommand.ResultType.STATE_FOUND) {
			showCheckingResult(item, Checked.SUCCESS, "verifications.symbolicchecking.resultHandler.findValidState.result.found");
			item.setExample(cmd.getTrace(stateSpace));
		} else if (result == FindStateCommand.ResultType.NO_STATE_FOUND) {
			showCheckingResult(item, Checked.FAIL, "verifications.symbolicchecking.resultHandler.findValidState.result.notFound");
		} else if (result == FindStateCommand.ResultType.INTERRUPTED) {
			showCheckingResult(item, Checked.INTERRUPTED, "verifications.symbolicchecking.resultHandler.findValidState.result.interrupted");
		} else {
			showCheckingResult(item, Checked.FAIL, "verifications.symbolicchecking.resultHandler.findValidState.result.error");
		}
	}
	
	public void handleFindRedundantInvariants(SymbolicAnimationFormulaItem item, GetRedundantInvariantsCommand cmd) {
		List<String> result = cmd.getRedundantInvariants();
		if(cmd.isInterrupted()) {
			showCheckingResult(item, Checked.INTERRUPTED, "verifications.interrupted");
		} else if (result.isEmpty()) {
			showCheckingResult(item, Checked.SUCCESS, 
					"verifications.symbolicchecking.resultHandler.findRedundantInvariants.result.notFound");
		} else {
			final String headerKey = cmd.isTimeout() ? "verifications.symbolicchecking.resultHandler.findRedundantInvariants.result.timeout" : "verifications.symbolicchecking.resultHandler.findRedundantInvariants.result.found";
			showCheckingResult(item, Checked.FAIL, headerKey, GENERAL_RESULT_MESSAGE, String.join("\n", result));
		}
	}
	
	private void showCheckingResult(SymbolicAnimationFormulaItem item, Checked checked, String headerKey, String msgKey, Object... msgParams) {
		item.setResultItem(new CheckingResultItem(checked, headerKey, msgKey, msgParams ));
		handleItem(item, checked);
	}
	
	private void showCheckingResult(SymbolicAnimationFormulaItem item, Checked checked, String msgKey) {
		showCheckingResult(item, checked, msgKey, msgKey);
	}
	
	protected void handleItem(AbstractCheckableItem item, Checked checked) {
		item.setChecked(checked);
		if(checked == Checked.SUCCESS) {
			item.setCheckedSuccessful();
		} else if(checked == Checked.FAIL) {
			item.setCheckedFailed();
		} else if(checked == Checked.INTERRUPTED || checked == Checked.TIMEOUT) {
			item.setCheckInterrupted();
		}
	}
	
	public void handleFormulaResult(SymbolicFormulaItem item, Object result) {
		Class<?> clazz = result.getClass();
		if(success.contains(clazz)) {
			handleItem(item, Checked.SUCCESS);
		} else if(error.contains(clazz) || result instanceof Throwable) {
			handleItem(item, Checked.FAIL);
		} else {
			handleItem(item, Checked.INTERRUPTED);
		}
		CheckingResultItem resultItem = handleFormulaResult(result);
		item.setResultItem(resultItem);
	}
	
	public CheckingResultItem handleFormulaResult(Object result) {
		CheckingResultItem resultItem = null;
		if(success.contains(result.getClass())) {
			resultItem = new CheckingResultItem(Checked.SUCCESS, "animation.symbolic.result.succeeded.header",
					"animation.symbolic.result.succeeded.message");
		} else if(error.contains(result.getClass())) {
			resultItem = new CheckingResultItem(Checked.FAIL, "verifications.result.error.header",
					GENERAL_RESULT_MESSAGE, ((IModelCheckingResult) result).getMessage());
		} else if(result instanceof Throwable) {
			resultItem = new CheckingResultItem(Checked.FAIL, "verifications.result.couldNotParseFormula.header",
					GENERAL_RESULT_MESSAGE, result);
		} else if(interrupted.contains(result.getClass())) {
			resultItem = new CheckingResultItem(Checked.INTERRUPTED, "verifications.result.interrupted.header",
					GENERAL_RESULT_MESSAGE, ((IModelCheckingResult) result).getMessage());
		}
		return resultItem;
	}
	
	public void handleFormulaResult(SymbolicFormulaItem item, AbstractCommand cmd) {
		StateSpace stateSpace = currentTrace.getStateSpace();
		if(item.getType() == SymbolicExecutionType.FIND_VALID_STATE) {
			handleFindValidState((SymbolicAnimationFormulaItem) item, (FindStateCommand) cmd, stateSpace);
		} else if(item.getType() == SymbolicExecutionType.FIND_REDUNDANT_INVARIANTS) {
			handleFindRedundantInvariants((SymbolicAnimationFormulaItem) item, (GetRedundantInvariantsCommand) cmd);
		}
	}
	
	public void showAlreadyExists(AbstractResultHandler.ItemType itemType) {
		stageManager.makeAlert(AlertType.INFORMATION,
				"verifications.abstractResultHandler.alerts.alreadyExists.header",
				"verifications.abstractResultHandler.alerts.alreadyExists.content", bundle.getString(itemType.getKey()))
				.showAndWait();
	}
	
	public void showResult(SymbolicAnimationFormulaItem item) {
		CheckingResultItem resultItem = item.getResultItem();
		if(resultItem == null || item.getChecked() == Checked.SUCCESS) {
			return;
		}
		Alert alert = stageManager.makeAlert(
				resultItem.getChecked().equals(Checked.SUCCESS) ? AlertType.INFORMATION : AlertType.ERROR,
				resultItem.getHeaderBundleKey(),
				resultItem.getMessageBundleKey(), resultItem.getMessageParams());
		alert.setTitle(item.getName());
		alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
		alert.showAndWait();
	}

}