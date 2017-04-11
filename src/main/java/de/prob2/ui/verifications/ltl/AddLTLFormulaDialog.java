package de.prob2.ui.verifications.ltl;

import com.google.inject.Inject;
import com.google.inject.Injector;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import de.prob2.ui.internal.StageManager;
import javafx.fxml.FXML;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.Dialog;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;
import javafx.stage.Modality;

public class AddLTLFormulaDialog extends Dialog<LTLFormulaItem> {
	
	@FXML
	private TextField tf_name;
	
	@FXML
	private TextArea ta_description;

	@Inject
	public AddLTLFormulaDialog(final StageManager stageManager, final Injector injector) {
		super();
		this.setResultConverter(type -> {
			if(type == null || type.getButtonData() == ButtonBar.ButtonData.CANCEL_CLOSE) {
				return null;
			} else {
				LTLFormulaStage formulaStage = injector.getInstance(LTLFormulaStage.class);
				formulaStage.setTitle(tf_name.getText());
				FontAwesomeIconView image = new FontAwesomeIconView(FontAwesomeIcon.QUESTION_CIRCLE);
				image.setFill(Color.BLUE);
				return new LTLFormulaItem(image, tf_name.getText(), ta_description.getText(), formulaStage);
			}
		});
		this.initModality(Modality.APPLICATION_MODAL);
		stageManager.loadFXML(this, "ltlformula_dialog.fxml");
	}
	
}
