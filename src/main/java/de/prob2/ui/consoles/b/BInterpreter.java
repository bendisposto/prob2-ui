package de.prob2.ui.consoles.b;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

import de.prob.animator.command.EvaluationCommand;
import de.prob.animator.domainobjects.AbstractEvalResult;
import de.prob.animator.domainobjects.ClassicalB;
import de.prob.animator.domainobjects.EvaluationException;
import de.prob.animator.domainobjects.IEvalElement;
import de.prob.scripting.ClassicalBFactory;
import de.prob.scripting.ModelTranslationError;
import de.prob.statespace.AnimationSelector;
import de.prob.statespace.FormalismType;
import de.prob.statespace.IAnimationChangeListener;
import de.prob.statespace.StateSpace;
import de.prob.statespace.Trace;
import de.prob2.ui.consoles.ConsoleInstruction;
import de.prob2.ui.consoles.Executable;


public class BInterpreter implements IAnimationChangeListener, Executable<String> {

	private static final Logger logger = LoggerFactory.getLogger(BInterpreter.class);
	private final StateSpace defaultSS;
	private String modelName;
	private Trace currentTrace;

	@Inject
	public BInterpreter(final ClassicalBFactory bfactory, final AnimationSelector animations) {
		StateSpace s = null;
		try {
			s = bfactory.create("MACHINE Empty END").load();
		} catch (ModelTranslationError e) {
			logger.error("loading a model into ProB failed!", e);
		}
		defaultSS = s;
		animations.registerAnimationChangeListener(this);
	}

	public String exec(final ConsoleInstruction instruction) {
		String line = instruction.getInstruction();
		String res = "";
		try {
			IEvalElement parsed = parse(line);
			if (currentTrace == null) {
				EvaluationCommand cmd = parsed.getCommand(defaultSS.getRoot());
				defaultSS.execute(cmd);
				res = cmd.getValue().toString();
			} else {
				AbstractEvalResult result = currentTrace.evalCurrent(parsed);
				res = result.toString();
			}
		} catch (EvaluationException e) {
			logger.info("B evaluation failed", e);
			return "Invalid syntax: " + e.getMessage();
		}
		return res;
	}

	public String result(AbstractEvalResult res) {
		return res.toString();
	}
	
	public IEvalElement parse(final String line) {
		if (currentTrace == null) {
			return new ClassicalB(line);
		}
		return currentTrace.getModel().parseFormula(line);
	}
		
	@Override
	public void traceChange(final Trace currentTrace, final boolean currentAnimationChanged) {
		if (currentAnimationChanged) {
			if (currentTrace == null) {
				modelName = null;
				notifyModelChange(modelName);
			} else if (currentTrace.getModel().getFormalismType() == FormalismType.B) {
				// ignore models that are not B models
				String modelName = currentTrace.getStateSpace().getMainComponent().toString();
				if (!modelName.equals(this.modelName)) {
					this.modelName = modelName;
					notifyModelChange(this.modelName);
				}
			}
			this.currentTrace = currentTrace;
		}
	}

	public void notifyModelChange(final String name) {
		//submit(WebUtils.wrap("cmd", "BConsole.modelChange", "modelloaded", name != null, "name", name == null ? "" : name));
		logger.trace("BConsole.modelChange\n modelloaded");
		String output = name == null ? "" : name;
		logger.trace(output);
	}

	@Override
	public void animatorStatus(final boolean busy) {
		if (busy) {
			//submit(WebUtils.wrap("cmd", "BConsole.disable"));
			logger.trace("BConsole.disable");
		} else {
			//submit(WebUtils.wrap("cmd", "BConsole.enable"));
			logger.trace("BConsole.enable");
		}
	}


}
