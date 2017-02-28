package de.prob2.ui.states;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.prob.animator.domainobjects.AbstractEvalResult;
import de.prob.animator.domainobjects.EvalResult;
import de.prob.animator.domainobjects.EvaluationErrorResult;
import de.prob.animator.domainobjects.EvaluationException;
import de.prob.animator.domainobjects.IEvalElement;
import de.prob.animator.domainobjects.StateError;
import de.prob.exception.ProBError;
import de.prob.model.representation.AbstractElement;
import de.prob.model.representation.AbstractFormulaElement;
import de.prob.model.representation.Machine;
import de.prob.statespace.Trace;
import de.prob2.ui.formula.FormulaGenerator;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentTrace;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.SetChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableRow;
import javafx.scene.control.TreeTableView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.AnchorPane;
import javafx.util.Callback;

@Singleton
public final class StatesView extends AnchorPane {
	private static final Logger logger = LoggerFactory.getLogger(StatesView.class);
	
	@FXML private TreeTableView<Object> tv;
	@FXML private TreeTableColumn<Object, Object> tvName;
	@FXML private TreeTableColumn<Object, Object> tvValue;
	@FXML private TreeTableColumn<Object, Object> tvPreviousValue;
	@FXML private TreeItem<Object> tvRootItem;

	private final Injector injector;
	private final CurrentTrace currentTrace;
	private final ClassBlacklist classBlacklist;
	private final FormulaGenerator formulaGenerator;
	private final StageManager stageManager;

	private final Map<IEvalElement, AbstractEvalResult> currentValues;
	private final Map<IEvalElement, AbstractEvalResult> previousValues;

	@Inject
	private StatesView(
		final Injector injector,
		final CurrentTrace currentTrace,
		final ClassBlacklist classBlacklist,
		final FormulaGenerator formulaGenerator,
		final StageManager stageManager
	) {
		this.injector = injector;
		this.currentTrace = currentTrace;
		this.classBlacklist = classBlacklist;
		this.formulaGenerator = formulaGenerator;
		this.stageManager = stageManager;
		
		this.currentValues = new HashMap<>();
		this.previousValues = new HashMap<>();

		stageManager.loadFXML(this, "states_view.fxml");
	}

	private void unsubscribeAllChildren(final Trace trace, final AbstractElement element) {
		if (element instanceof AbstractFormulaElement) {
			((AbstractFormulaElement) element).unsubscribe(trace.getStateSpace());
		}
		
		for (final Class<? extends AbstractElement> clazz : element.getChildren().keySet()) {
			this.unsubscribeAllChildren(trace, element.getChildren().get(clazz));
		}
	}

	private void unsubscribeAllChildren(final Trace trace, final List<? extends AbstractElement> elements) {
		for (AbstractElement e : elements) {
			this.unsubscribeAllChildren(trace, e);
		}
	}

	private void updateElements(final Trace trace, final TreeItem<Object> treeItem, final List<? extends AbstractElement> elements) {
		for (AbstractElement e : elements) {
			if (e instanceof AbstractFormulaElement) {
				((AbstractFormulaElement) e).subscribe(trace.getStateSpace());
			}
		}

		this.currentValues.clear();
		this.currentValues.putAll(trace.getCurrentState().getValues());
		this.previousValues.clear();
		if (trace.canGoBack()) {
			this.previousValues.putAll(trace.getPreviousState().getValues());
		}

		for (AbstractElement e : elements) {
			TreeItem<Object> childItem = null;

			for (TreeItem<Object> ti : treeItem.getChildren()) {
				if (ti.getValue().equals(e)) {
					childItem = ti;
					childItem.setValue(e);
					break;
				}
			}

			if (childItem == null) {
				childItem = new TreeItem<>(e);
				treeItem.getChildren().add(childItem);
			}

			this.updateChildren(trace, childItem, e);
		}

		Iterator<TreeItem<Object>> it = treeItem.getChildren().iterator();
		while (it.hasNext()) {
			TreeItem<Object> ti = it.next();
			if (ti.getValue() instanceof AbstractElement) {
				AbstractElement element = (AbstractElement)ti.getValue();
				if (!elements.contains(element)) {
					it.remove();
					if (element instanceof AbstractFormulaElement) {
						((AbstractFormulaElement) element).unsubscribe(trace.getStateSpace());
					}
				}
			} else {
				it.remove();
			}
		}

		treeItem.getChildren().sort(Comparator.comparing(a -> NameCell.getName(a.getValue())));
	}

	private void updateChildren(final Trace trace, final TreeItem<Object> treeItem, final AbstractElement element) {
		this.classBlacklist.getKnownClasses().addAll(element.getChildren().keySet());
		for (Class<? extends AbstractElement> clazz : element.getChildren().keySet()) {
			if (this.classBlacklist.getBlacklist().contains(clazz)) {
				continue;
			}

			TreeItem<Object> childItem = null;
			for (TreeItem<Object> ti : treeItem.getChildren()) {
				if (ti.getValue().equals(clazz)) {
					childItem = ti;
					break;
				}
			}

			if (childItem == null) {
				childItem = new TreeItem<>(clazz);
				treeItem.getChildren().add(childItem);
			}

			this.updateElements(trace, childItem, element.getChildren().get(clazz));
		}

		Iterator<TreeItem<Object>> it = treeItem.getChildren().iterator();
		while (it.hasNext()) {
			TreeItem<Object> ti = it.next();
			Object sti = ti.getValue();
			if (sti instanceof Class<?> && AbstractElement.class.isAssignableFrom((Class<?>)sti)) {
				Class<? extends AbstractElement> clazz = ((Class<?>)sti).asSubclass(AbstractElement.class);
				if (!element.getChildren().containsKey(clazz) || this.classBlacklist.getBlacklist().contains(clazz)) {
					this.unsubscribeAllChildren(trace, element.getChildren().get(clazz));
					it.remove();
				}
			} else {
				it.remove();
			}
		}

		treeItem.getChildren().sort(Comparator.comparing(a -> NameCell.getName(a.getValue())));
	}

	private void updateRoot(final Trace trace) {
		int row = tv.getSelectionModel().getSelectedIndex();
		this.updateElements(trace, this.tvRootItem, trace.getModel().getChildrenOfType(Machine.class));
		
		final TreeItem<Object> errorsItem = new TreeItem<>(StateError.class);
		
		for (final StateError error : trace.getCurrentState().getStateErrors()) {
			errorsItem.getChildren().add(new TreeItem<>(error));
		}
		
		this.tvRootItem.getChildren().add(errorsItem);
		
		for (final TreeItem<?> child : this.tvRootItem.getChildren()) {
			child.setExpanded(true);
		}
		
		this.tv.refresh();
		this.tv.getSelectionModel().select(row);
	}

	private void visualizeExpression(AbstractFormulaElement formula) {
		try {
			formulaGenerator.showFormula(formula.getFormula());
		} catch (EvaluationException | ProBError e) {
			logger.error("Could not visualize formula", e);
			stageManager.makeAlert(Alert.AlertType.ERROR, "Could not visualize formula:\n" + e).showAndWait();
		}
	}

	@FXML
	private void initialize() {
		tv.setRowFactory(view -> { // NOSONAR // Sonar counts every if statement in a lambda as a conditional expression and complains if there are more than 3. This is not a reasonable limit here.
			final TreeTableRow<Object> row = new TreeTableRow<>();
			
			row.itemProperty().addListener((observable, from, to) -> {
				row.getStyleClass().remove("changed");
				if (to instanceof AbstractFormulaElement) {
					final IEvalElement formula = ((AbstractFormulaElement)to).getFormula();
					final AbstractEvalResult current = this.currentValues.get(formula);
					final AbstractEvalResult previous = this.previousValues.get(formula);
					
					if (current != null && previous != null && (
						!current.getClass().equals(previous.getClass())
						|| current instanceof EvalResult && !((EvalResult)current).getValue().equals(((EvalResult)previous).getValue())
					)) {
						row.getStyleClass().add("changed");
					}
				}
			});
			
			final MenuItem visualizeExpressionItem = new MenuItem("Visualize Expression");
			// Expression can only be shown if the row item is an AbstractFormulaElement and the current state is initialized.
			visualizeExpressionItem.disableProperty().bind(
				Bindings.createBooleanBinding(() -> !(row.getItem() instanceof AbstractFormulaElement), row.itemProperty())
				.or(currentTrace.currentStateProperty().initializedProperty().not())
			);
			visualizeExpressionItem.setOnAction(event ->
				visualizeExpression((AbstractFormulaElement)row.getItem())
			);
			
			final MenuItem showFullValueItem = new MenuItem("Show Full Value");
			// Full value can only be shown if the row item is any of the following:
			// * An AbstractFormulaElement, and the corresponding value is an EvalResult.
			// * A StateError
			showFullValueItem.disableProperty().bind(Bindings.createBooleanBinding(
				() -> !(
					row.getItem() instanceof AbstractFormulaElement
					&& this.currentValues.get(((AbstractFormulaElement)row.getItem()).getFormula()) instanceof EvalResult
					|| row.getItem() instanceof StateError
				),
				row.itemProperty()
			));
			showFullValueItem.setOnAction(event -> {
				final FullValueStage stage = injector.getInstance(FullValueStage.class);
				if (row.getItem() instanceof AbstractFormulaElement) {
					final AbstractFormulaElement element = (AbstractFormulaElement)row.getItem();
					final EvalResult currentResult = (EvalResult)this.currentValues.get(element.getFormula());
					stage.setTitle(element.toString());
					stage.setCurrentValue(AsciiUnicodeString.fromAscii(currentResult.getValue()));
					if (this.previousValues != null && this.previousValues.get(element.getFormula()) instanceof EvalResult) {
						final EvalResult previousResult = (EvalResult)this.previousValues.get(element.getFormula());
						stage.setPreviousValue(AsciiUnicodeString.fromAscii(previousResult.getValue()));
					} else {
						stage.setPreviousValue(null);
					}
					stage.setFormattingEnabled(true);
				} else if (row.getItem() instanceof StateError) {
					final StateError error = (StateError)row.getItem();
					stage.setTitle(error.getEvent());
					stage.setCurrentValue(AsciiUnicodeString.fromAscii(error.getLongDescription()));
					stage.setPreviousValue(null);
					stage.setFormattingEnabled(false);
				} else {
					throw new IllegalArgumentException("Invalid row item type: " + row.getItem().getClass());
				}
				stage.show();
			});
			
			final MenuItem showErrorsItem = new MenuItem("Show Errors");
			// Errors can only be shown if the row is an AbstractFormulaElement whose value is an EvaluationErrorResult.
			showErrorsItem.disableProperty().bind(Bindings.createBooleanBinding(
				() -> !(
					row.getItem() instanceof AbstractFormulaElement
					&& this.currentValues.get(((AbstractFormulaElement)row.getItem()).getFormula()) instanceof EvaluationErrorResult
				),
				row.itemProperty()
			));
			showErrorsItem.setOnAction(event -> {
				final FullValueStage stage = injector.getInstance(FullValueStage.class);
				if (row.getItem() instanceof AbstractFormulaElement) {
					final AbstractEvalResult result = this.currentValues.get(((AbstractFormulaElement)row.getItem()).getFormula()); 
					if (result instanceof EvaluationErrorResult) {
						stage.setTitle(row.getItem().toString());
						stage.setCurrentValue(AsciiUnicodeString.fromAscii(String.join("\n", ((EvaluationErrorResult)result).getErrors())));
						stage.setFormattingEnabled(false);
						stage.show();
					} else {
						throw new IllegalArgumentException("Row item result is not an error: " + result.getClass());
					}
				} else {
					throw new IllegalArgumentException("Invalid row item type: " + row.getItem().getClass());
				}
			});
			
			row.contextMenuProperty().bind(
				Bindings.when(row.emptyProperty())
				.then((ContextMenu) null)
				.otherwise(new ContextMenu(visualizeExpressionItem, showFullValueItem, showErrorsItem))
			);
			
			// Double-click on an item triggers "show full value" if allowed.
			row.setOnMouseClicked(event -> {
				if (!showFullValueItem.isDisable() && event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
					showFullValueItem.getOnAction().handle(null);
				}
			});
			
			return row;
		});
		
		this.tvName.setCellFactory(col -> new NameCell());
		this.tvValue.setCellFactory(col -> new ValueCell(this.currentValues, true));
		this.tvPreviousValue.setCellFactory(col -> new ValueCell(this.previousValues, false));

		final Callback<TreeTableColumn.CellDataFeatures<Object, Object>, ObservableValue<Object>> cellValueFactory = data -> Bindings.createObjectBinding(data.getValue()::getValue, this.currentTrace);
		this.tvName.setCellValueFactory(cellValueFactory);
		this.tvValue.setCellValueFactory(cellValueFactory);
		this.tvPreviousValue.setCellValueFactory(cellValueFactory);

		this.tvRootItem.setValue(Machine.class);

		this.classBlacklist.getBlacklist()
				.addListener((SetChangeListener<? super Class<? extends AbstractElement>>) change -> {
					if (this.currentTrace.exists()) {
						this.updateRoot(this.currentTrace.get());
					}
				});

		final ChangeListener<Trace> traceChangeListener = (observable, from, to) -> {
			if (to == null) {
				this.tvRootItem.getChildren().clear();
			} else {
				this.updateRoot(to);
			}
		};
		traceChangeListener.changed(this.currentTrace, null, currentTrace.get());
		this.currentTrace.addListener(traceChangeListener);
	}
	
	public TreeTableView<Object> getTable() {
		return tv;
	}
}
