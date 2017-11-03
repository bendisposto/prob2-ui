package de.prob2.ui.plugin;

import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob2.ui.internal.StageManager;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.MouseButton;
import javafx.stage.Modality;
import javafx.stage.Stage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ro.fortsoft.pf4j.PluginState;
import ro.fortsoft.pf4j.PluginStateListener;
import ro.fortsoft.pf4j.PluginWrapper;

/**
 * Created by Christoph Heinzen on 03.08.17.
 */
@Singleton
public class PluginMenuStage extends Stage {

	private static final Logger LOGGER = LoggerFactory.getLogger(PluginMenuStage.class);

	private final ProBPluginManager proBPluginManager;
	private final ResourceBundle bundle;
	private final StageManager stageManager;

	private ObservableList<PluginWrapper> pluginList;

	@FXML
	private TableView<PluginWrapper> pluginTableView;
	@FXML
	private TextField pluginSearchTextField;
	@FXML
	private TableColumn<PluginWrapper, String> nameCol;
	@FXML
	private TableColumn<PluginWrapper, String> versionCol;
	@FXML
	private TableColumn<PluginWrapper, Boolean> activeCol;
	@FXML
	private TextField pathTextField;

	private PluginStateListener stateListener;

	@Inject
	public PluginMenuStage(final StageManager stageManager,
						   final ProBPluginManager proBPluginManager,
						   final ResourceBundle bundle) {
		this.proBPluginManager = proBPluginManager;
		this.bundle = bundle;
		this.stageManager = stageManager;
		stageManager.loadFXML(this, "plugin_menu_stage.fxml", this.getClass().getName());
		initModality(Modality.APPLICATION_MODAL);
		initOwner(stageManager.getMainStage());
	}

	@FXML
	private void initialize() {
		//TODO: check how to sort a tableview proper
		pathTextField.setText(proBPluginManager.getPluginDirectory().getAbsolutePath());

		pluginList = FXCollections.observableArrayList(getProBJarPluginManager().getPlugins());
		pluginList.sort(Comparator.comparing(pluginWrapper -> ((ProBPlugin) pluginWrapper.getPlugin()).getName()));

		stateListener = event -> {
			PluginWrapper plugin = event.getPlugin();
			if (pluginList.contains(plugin) && !getProBJarPluginManager().getPlugins().contains(plugin)) {
				//a plugin was removed
				pluginList.remove(plugin);
			} else if (!pluginList.contains(plugin) && getProBJarPluginManager().getPlugins().contains(plugin)) {
				//a new plugin was added
				pluginList.add(plugin);
			}
		};

		getProBJarPluginManager().addPluginStateListener(stateListener);
		//TODO: check listener and list handling when stage not shown
		//this.setOnCloseRequest(closeEvent -> getProBJarPluginManager().removePluginStateListener(stateListener));
		//this.setOnShown(event -> {System.out.println();});

		configureColumns();
		configureContextMenu();

		FilteredList<PluginWrapper> pluginFilteredList = new FilteredList<>(pluginList, p -> true);
		pluginSearchTextField.textProperty().addListener((observable, oldValue, newValue) ->
				pluginFilteredList.setPredicate(plugin -> {
					if (newValue == null || newValue.isEmpty()) {
						return true;
					}
					return ((ProBPlugin) plugin.getPlugin()).getName().toLowerCase().contains(newValue.toLowerCase());
				}));

		SortedList<PluginWrapper> pluginSortedFilteredList = new SortedList<>(pluginFilteredList);
		pluginSortedFilteredList.comparatorProperty().bind(pluginTableView.comparatorProperty());
		pluginTableView.setItems(pluginSortedFilteredList);
	}

	@FXML
	private void addPlugin() {
	   proBPluginManager.addPlugin();
	}

	@FXML
	private void reloadPlugins() {
		proBPluginManager.reloadPlugins();
		List<PluginWrapper> plugins = getProBJarPluginManager().getPlugins();
		plugins.sort(Comparator.comparing(pluginWrapper -> ((ProBPlugin) pluginWrapper.getPlugin()).getName()));
		pluginList.clear();
		pluginList.addAll(plugins);
	}

	@FXML
	private void changePath() {
		List<PluginWrapper> plugins = proBPluginManager.changePluginDirectory();
		if (plugins != null) {
			pluginList.clear();
			pluginList.addAll(plugins);
			getProBJarPluginManager().addPluginStateListener(stateListener);
		}
		pathTextField.setText(proBPluginManager.getPluginDirectory().getAbsolutePath());
	}

	private void configureColumns() {
		nameCol.setCellFactory(TextFieldTableCell.forTableColumn());
		nameCol.setCellValueFactory(param -> new SimpleStringProperty(((ProBPlugin)param.getValue().getPlugin()).getName()));
		nameCol.setSortType(TableColumn.SortType.ASCENDING);

		versionCol.setCellFactory(param -> {
			TextFieldTableCell<PluginWrapper, String> cell = new TextFieldTableCell<>();
			cell.setAlignment(Pos.CENTER);
			return cell;
		});
		versionCol.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getDescriptor().getVersion().toString()));

		activeCol.setCellFactory(param -> {
			CheckBoxTableCell<PluginWrapper, Boolean> cell = new CheckBoxTableCell<>();
			cell.setAlignment(Pos.CENTER);
			return cell;
		});
		activeCol.setCellValueFactory(param -> {
			final PluginWrapper plugin = param.getValue();
			SimpleBooleanProperty booleanProp = new SimpleBooleanProperty(plugin.getPluginState() == PluginState.STARTED);
			booleanProp.addListener((observable, oldValue, newValue) -> {
				if (newValue) {
					getProBJarPluginManager().startPlugin(plugin.getPluginId());
				} else {
					getProBJarPluginManager().stopPlugin(plugin.getPluginId());
				}
				proBPluginManager.writeInactivePlugins();
			});
			return booleanProp;
		});
	}

	private void configureContextMenu() {
		pluginTableView.setRowFactory(tableView -> {
			final TableRow<PluginWrapper> row = new TableRow<>();
			row.setOnMouseClicked(clickEvent -> {
				if(clickEvent.getButton() == MouseButton.SECONDARY && row.getItem() != null) {
					ContextMenu ctMenu = createContextMenu(row.getItem());
					ctMenu.show(row, clickEvent.getScreenX(), clickEvent.getScreenY());
				}
			});
			return row;
		});
	}

	private ContextMenu createContextMenu(PluginWrapper pluginWrapper) {
		ProBPlugin plugin = (ProBPlugin) pluginWrapper.getPlugin();
		String pluginId = pluginWrapper.getPluginId();
		String pluginName = plugin.getName();

		MenuItem restartItem = new MenuItem(
				getFormattedString("pluginsmenu.table.contextmenu.restart", pluginName));
		restartItem.setOnAction(event -> {
			if (PluginState.STOPPED == getProBJarPluginManager().stopPlugin(pluginId)) {
				getProBJarPluginManager().startPlugin(pluginId);
			}
		});

		MenuItem removeMenuItem = new MenuItem(
				getFormattedString("pluginsmenu.table.contextmenu.remove", pluginName));
		removeMenuItem.setOnAction(event -> {
			Alert dialog = stageManager.makeAlert(Alert.AlertType.CONFIRMATION,
					getFormattedString("pluginsmenu.table.dialog.remove.question", pluginName),
					ButtonType.NO, ButtonType.YES);
			dialog.initOwner(this);
			dialog.setTitle(bundle.getString("pluginsmenu.table.dialog.title"));
			dialog.showAndWait().ifPresent(response -> {
				if (response == ButtonType.YES) {
					getProBJarPluginManager().deletePlugin(pluginId);
				}
			});
		});
		return new ContextMenu(restartItem, removeMenuItem);
	}

	private ProBPluginManager.ProBJarPluginManager getProBJarPluginManager() {
		return proBPluginManager.getPluginManager();
	}

	private String getFormattedString(String key, Object... args) {
		String bundleString = bundle.getString(key);
		if (args.length == 0) {
			return bundleString;
		} else {
			return String.format(bundleString, args);
		}
	}
}
