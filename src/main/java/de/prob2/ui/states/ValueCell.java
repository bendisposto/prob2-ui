package de.prob2.ui.states;

import java.util.Map;
import java.util.ResourceBundle;

import de.prob.animator.domainobjects.AbstractEvalResult;
import de.prob.animator.domainobjects.EnumerationWarning;
import de.prob.animator.domainobjects.EvalResult;
import de.prob.animator.domainobjects.EvaluationErrorResult;
import de.prob.animator.domainobjects.IEvalElement;
import de.prob.animator.domainobjects.IdentifierNotInitialised;
import de.prob.animator.domainobjects.StateError;
import de.prob.animator.domainobjects.WDError;
import de.prob.animator.prologast.ASTCategory;
import de.prob.animator.prologast.ASTFormula;

import javafx.scene.control.OverrunStyle;
import javafx.scene.control.TreeTableCell;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class ValueCell extends TreeTableCell<StateItem<?>, StateItem<?>> {
	private static final Logger LOGGER = LoggerFactory.getLogger(ValueCell.class);
	
	private final ResourceBundle bundle;
	private final Map<IEvalElement, AbstractEvalResult> values;
	private final boolean isCurrent;
	
	ValueCell(final ResourceBundle bundle, final Map<IEvalElement, AbstractEvalResult> values, final boolean isCurrent) {
		super();
		
		this.bundle = bundle;
		this.values = values;
		this.isCurrent = isCurrent;
		
		this.setTextOverrun(OverrunStyle.CENTER_WORD_ELLIPSIS);
	}
	
	@Override
	protected void updateItem(final StateItem<?> item, final boolean empty) {
		super.updateItem(item, empty);
		
		this.getStyleClass().removeAll("false", "true", "errorresult");
		
		if (item == null || empty) {
			this.setText(null);
			this.setGraphic(null);
		} else {
			final Object contents = item.getContents();
			
			if (contents instanceof String || contents instanceof ASTCategory) {
				this.setText(null);
			} else if (contents instanceof ASTFormula) {
				final AbstractEvalResult result = this.values.get(((ASTFormula)contents).getFormula());
				checkResult(result);
			} else if (contents instanceof StateError) {
				this.setText(this.isCurrent ? ((StateError)contents).getShortDescription() : null);
				this.getStyleClass().add("errorresult");
			} else {
				throw new IllegalArgumentException("Don't know how to show the value of a " + contents.getClass() + " instance");
			}
			this.setGraphic(null);
		}
	}

	private void checkResult(final AbstractEvalResult result) {
		if (result == null) {
			this.setText(null);
		} else if (result instanceof EvalResult) {
			final EvalResult eresult = (EvalResult)result;
			this.setText(eresult.getValue());
			if ("FALSE".equals(eresult.getValue())) {
				this.getStyleClass().add("false");
			} else if ("TRUE".equals(eresult.getValue())) {
				this.getStyleClass().add("true");
			}
		} else if (result instanceof IdentifierNotInitialised) {
			this.setText(bundle.getString("states.value.notInitialized"));
			this.getStyleClass().add("errorresult");
		} else if (result instanceof WDError) {
			this.setText(bundle.getString("states.value.notWellDefined"));
			this.getStyleClass().add("errorresult");
		} else if (result instanceof EvaluationErrorResult) {
			this.setText(String.format(bundle.getString("states.value.error"), ((EvaluationErrorResult)result).getResult()));
			this.getStyleClass().add("errorresult");
		} else if (result instanceof EnumerationWarning) {
			this.setText(bundle.getString("states.value.enumerationWarning"));
			this.getStyleClass().add("errorresult");
		} else {
			throw new IllegalArgumentException("Don't know how to show the value of a " + result.getClass() + " instance");
		}
	}
}
