package de.prob2.ui.consoles.groovy;

import javax.script.ScriptEngine;
import javax.script.ScriptException;

import com.google.inject.Inject;

import de.prob.scripting.ScriptEngineProvider;
import de.prob2.ui.consoles.ConsoleExecResult;
import de.prob2.ui.consoles.ConsoleInstruction;
import de.prob2.ui.consoles.Executable;
import de.prob2.ui.consoles.groovy.codecompletion.GroovyCodeCompletion;
import de.prob2.ui.consoles.groovy.codecompletion.CodeCompletionTriggerAction;
import de.prob2.ui.consoles.groovy.objects.GroovyObjectStage;
import javafx.fxml.FXMLLoader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.codehaus.groovy.GroovyBugError;

public class GroovyInterpreter implements Executable {
	private static final Logger logger = LoggerFactory.getLogger(GroovyInterpreter.class);
	private final ScriptEngine engine;
	private final GroovyCodeCompletion codeCompletion;
	private final GroovyObjectStage groovyObjectStage;

	@Inject
	public GroovyInterpreter(FXMLLoader loader, final ScriptEngineProvider sep, final GroovyObjectStage groovyObjectStage) {
		engine = sep.get();
		this.groovyObjectStage = groovyObjectStage;
		this.codeCompletion = new GroovyCodeCompletion(loader, engine);
	}
	
	@Override
	public ConsoleExecResult exec(final ConsoleInstruction instruction) {
		logger.trace("Exec");
		
		if ("inspect".equals(instruction.getInstruction())) {
			groovyObjectStage.showObjects(engine);
			return new ConsoleExecResult("", "");
		} else if("clear".equals(instruction.getInstruction())) {
			return new ConsoleExecResult("clear","");
		} else {
			String resultString;
			StringBuilder console = new StringBuilder();
			engine.put("__console", console);
			logger.trace("Eval {} on {}", instruction.getInstruction(), engine);
			try {
				Object eval = engine.eval(instruction.getInstruction());
				resultString = eval.toString();
				logger.trace("Evaled {} to {}", instruction.getInstruction(), resultString);
			} catch (ScriptException|GroovyBugError e) {
				logger.debug("Groovy Evaluation failed", e);
				resultString = e.toString();
			}
	
			return new ConsoleExecResult(console.toString(), resultString);
		}
	}
	
	public void triggerCodeCompletion(GroovyConsole console, String currentLine, CodeCompletionTriggerAction action) {
		if(!codeCompletion.isVisible()) {
			codeCompletion.activate(console, currentLine, action);
		}
	}
	
	public void triggerCloseCodeCompletion() {
		codeCompletion.deactivate();
	}
	
	public void closeObjectStage() {
		groovyObjectStage.close();
	}
}