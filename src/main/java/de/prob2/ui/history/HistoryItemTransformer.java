package de.prob2.ui.history;

import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.util.Callback;

public class HistoryItemTransformer implements Callback<ListView<HistoryItem>, ListCell<HistoryItem>> {

	private class TransitionCell extends ListCell<HistoryItem> {

		@Override
		protected void updateItem(HistoryItem item, boolean empty) {
			super.updateItem(item, empty);
			if (item != null) {
				String content = item.transition.getPrettyRep();
				Text text = new Text(content);

				switch (item.status) {
				case FUTURE:
					text.setFont(Font.font("ARIAL", FontPosture.ITALIC, 12));
					text.setFill(Color.GRAY);
					break;
				case PRESENT:
					text.setFont(Font.font("ARIAL", FontWeight.BOLD, 12));
					break;
				default:
					break;
				}
				setGraphic(text);
			} else
				setGraphic(new Text(""));
		}

	}

	@Override
	public ListCell<HistoryItem> call(ListView<HistoryItem> param) {
		return new TransitionCell();
	}

}
