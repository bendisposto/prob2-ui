package de.prob2.ui.animation.symbolic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.prob.animator.command.ConstraintBasedSequenceCheckCommand;
import de.prob.animator.command.FindStateCommand;
import de.prob.animator.domainobjects.EventB;
import de.prob.animator.domainobjects.FormulaExpand;
import de.prob.statespace.StateSpace;
import de.prob.model.classicalb.ClassicalBModel;
import de.prob.model.representation.AbstractModel;
import de.prob2.ui.animation.symbolic.testcasegeneration.TestCaseGenerationFormulaExtractor;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.symbolic.SymbolicExecutionType;
import de.prob2.ui.symbolic.SymbolicFormulaHandler;
import de.prob2.ui.verifications.AbstractResultHandler;

import de.prob.analysis.testcasegeneration.ConstraintBasedTestCaseGenerator;

@Singleton
public class SymbolicAnimationFormulaHandler implements SymbolicFormulaHandler<SymbolicAnimationFormulaItem> {

	private final CurrentTrace currentTrace;
	
	private final SymbolicAnimationChecker symbolicChecker;
	
	private final SymbolicAnimationResultHandler resultHandler;
	
	private final Injector injector;
	
	private final CurrentProject currentProject;

	private final TestCaseGenerationFormulaExtractor extractor;
	
	@Inject
	public SymbolicAnimationFormulaHandler(final CurrentTrace currentTrace, final CurrentProject currentProject,
										   final Injector injector, final SymbolicAnimationChecker symbolicChecker,
										   final SymbolicAnimationResultHandler resultHandler, final TestCaseGenerationFormulaExtractor extractor) {
		this.currentTrace = currentTrace;
		this.currentProject = currentProject;
		this.injector = injector;
		this.symbolicChecker = symbolicChecker;
		this.resultHandler = resultHandler;
		this.extractor = extractor;
	}
	
	public void addFormula(String name, SymbolicExecutionType type, boolean checking) {
		SymbolicAnimationFormulaItem formula = new SymbolicAnimationFormulaItem(name, type);
		addFormula(formula,checking);
	}
	
	public void addFormula(SymbolicAnimationFormulaItem formula, boolean checking) {
		Machine currentMachine = currentProject.getCurrentMachine();
		if (currentMachine != null) {
			if(!currentMachine.getSymbolicAnimationFormulas().contains(formula)) {
				currentMachine.addSymbolicAnimationFormula(formula);
				injector.getInstance(SymbolicAnimationView.class).updateProject();
			} else if(!checking) {
				resultHandler.showAlreadyExists(AbstractResultHandler.ItemType.FORMULA);
			}
		}
	}
	
	public void handleSequence(SymbolicAnimationFormulaItem item, boolean checkAll) {
		List<String> events = Arrays.asList(item.getCode().replaceAll(" ", "").split(";"));
		ConstraintBasedSequenceCheckCommand cmd = new ConstraintBasedSequenceCheckCommand(currentTrace.getStateSpace(), events, new EventB("true", FormulaExpand.EXPAND));
		symbolicChecker.checkItem(item, cmd, currentTrace.getStateSpace(), checkAll);
	}
	
	public void findValidState(SymbolicAnimationFormulaItem item, boolean checkAll) {
		StateSpace stateSpace = currentTrace.getStateSpace();
		FindStateCommand cmd = new FindStateCommand(stateSpace, new EventB(item.getCode(), FormulaExpand.EXPAND), true);
		symbolicChecker.checkItem(item, cmd, stateSpace, checkAll);
	}

	public void generateTestCases(SymbolicAnimationFormulaItem item, boolean checkAll) {
		AbstractModel model = currentTrace.getModel();
		if(!(model instanceof ClassicalBModel)) {
			return;
		}
		ClassicalBModel bModel = (ClassicalBModel) model;
		StateSpace stateSpace = currentTrace.getStateSpace();
		ConstraintBasedTestCaseGenerator testCaseGenerator = new ConstraintBasedTestCaseGenerator(bModel, stateSpace, extractor.extractRawFormula(item.getCode()), Integer.parseInt(extractor.extractDepth(item.getCode())), new ArrayList<>());
		symbolicChecker.checkItem(item, testCaseGenerator, checkAll);
	}
	
	public void handleItem(SymbolicAnimationFormulaItem item, boolean checkAll) {
		if(!item.selected()) {
			return;
		}
		SymbolicExecutionType type = item.getType();
		switch(type) {
			case SEQUENCE:
				handleSequence(item, checkAll);
				break;
			case FIND_VALID_STATE:
				findValidState(item, checkAll);
				break;
			case MCDC:
				generateTestCases(item, checkAll);
				break;
			case COVERED_OPERATIONS:
				generateTestCases(item, checkAll);
				break;
			default:
				break;
		}
	}
	
	public void handleMachine(Machine machine) {
		machine.getSymbolicAnimationFormulas().forEach(item -> handleItem(item, true));
	}
	
}
