package player.fx.control;

import javafx.beans.property.StringProperty;
import javafx.event.ActionEvent;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.scene.shape.Rectangle;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

public class FileDropOverlay {
	private Pane region; // The component that is drop enabled

	private Group overlayRoot;
	private BorderPane cssPane, overlayPane;

	private Background cssBackground;

	private Label header;
	private List<ToggleButton> buttons = new ArrayList<>();
	private ToggleGroup toggleGroup = new ToggleGroup();

	private Function<List<File>, List<ToggleButton>> actionGenerator;

	public FileDropOverlay(Pane dropRegion) {
		super();
		region = dropRegion;

		// default action generator
		actionGenerator = files -> {
			return Arrays.asList(new ToggleButton("Action 1"), new ToggleButton("Action 2"),
					new ToggleButton("Action 3"));
		};

		overlayPane = new BorderPane();
		overlayPane.getStyleClass().addAll("button", "drop-overlay");
		overlayPane.prefWidthProperty().bind(region.widthProperty());
		overlayPane.prefHeightProperty().bind(region.heightProperty());
		overlayPane.setTop(header = new Label("Drop files on an item."));

		// the only purspose of the CSS pane is to capture the background style
		cssPane = new BorderPane(new Button());
		cssPane.getStyleClass().addAll("button", "drop-overlay");
		cssPane.setVisible(false);
		cssPane.setManaged(false);
		cssPane.backgroundProperty().addListener((p, o, n) -> {
			if (n != null)
				cssBackground = n;
		});
		region.getChildren().add(cssPane);

		overlayRoot = new Group(overlayPane);

		region.setOnDragEntered(event -> onDragEntered(event));
		region.setOnDragExited(event -> onDragExited(event));
		region.setOnDragOver(event -> onDragOver(event));
		region.setOnDragDropped(event -> onDragDropped(event));
	}

	public Function<List<File>, List<ToggleButton>> getActionGenerator() {
		return actionGenerator;
	}

	/**
	 * Sets the action generator for the overlay.
	 *
	 * The action generator is called to determine which options the user has
	 * when dragging files. It gets the list of dragged files and creates a list
	 * of {@link ToggleButton}s. The buttons should be initialized with display
	 * text and icon. When hovering over a button, the button's selected status
	 * will be set. Upon dropping the files, the button's action handler, set
	 * using {@link ToggleButton#setOnAction(javafx.event.EventHandler)}, will
	 * be informed.
	 *
	 * @param actionGenerator the action generator to use
	 */
	public void setActionGenerator(Function<List<File>, List<ToggleButton>> actionGenerator) {
		this.actionGenerator = actionGenerator;
	}

	public StringProperty headerTextProperty() {
		return header.textProperty();
	}

	public void setHeaderText(String headerText) {
		header.setText(headerText);
	}

	public String getHeaderText() {
		return header.getText();
	}

	public void dispose() {
		region.setOnDragEntered(null);
		region.setOnDragExited(null);
		region.setOnDragOver(null);
		region.setOnDragDropped(null);
		region.getChildren().remove(cssPane);
	}

	private void onDragEntered(DragEvent event) {
		List<File> files = event.getDragboard().getFiles();
		if (files == null)
			return;

		buttons.clear();
		toggleGroup.getToggles().clear();

		List<ToggleButton> choices;
		try {
			choices = actionGenerator.apply(new ArrayList<File>(files));
		} catch(Exception exc) {
			exc.printStackTrace();
			return;
		}
		buttons.addAll(choices);
		buttons.forEach(b -> toggleGroup.getToggles().add(b));

		Pane grid = layout(buttons, region);
		overlayPane.setCenter(grid);
		region.getChildren().add(overlayRoot);
	}

	private void onDragOver(DragEvent event) {
		Dragboard db = event.getDragboard();
		if (db.hasFiles()) {

			List<BackgroundFill> fills = new ArrayList<>(cssBackground.getFills());
			fills.set(fills.size() - 1, toRadial(fills.get(fills.size() - 1), event.getX(), event.getY()));
			overlayPane.setBackground(new Background(fills.toArray(new BackgroundFill[fills.size()])));

			Node pickResult = event.getPickResult().getIntersectedNode();
			if (pickResult instanceof ToggleButton) {
				((ToggleButton) pickResult).setSelected(true);
			}

			if (toggleGroup.getSelectedToggle() != null) {
				event.acceptTransferModes(TransferMode.COPY);
			}
		} else {
			event.consume();
		}
	}

	private BackgroundFill toRadial(BackgroundFill fill, double x, double y) {
		Paint paint = fill.getFill();

		if (paint instanceof LinearGradient) {
			LinearGradient lg = (LinearGradient) paint;
			paint = new RadialGradient(0, .1, x, y, 150, false, lg.getCycleMethod(), lg.getStops());
		} else if (paint instanceof Color) {
			Color c = (Color) paint;
			paint = new RadialGradient(0, .1, x, y, 150, false, CycleMethod.NO_CYCLE, new Stop(0, c.brighter()),
					new Stop(1, c));
		}

		return new BackgroundFill(paint, fill.getRadii(), fill.getInsets());
	}

	private void onDragDropped(DragEvent event) {
		ToggleButton button = (ToggleButton) toggleGroup.getSelectedToggle();
		if (button == null)
			return;

		if (button.getOnAction() != null) {
			ActionEvent e = new ActionEvent(event, button);
			try {
				button.getOnAction().handle(e);
			} catch(Exception exc) {
				exc.printStackTrace();
			}
		}

		hide();

		event.setDropCompleted(true);
		event.consume();
	}

	private void onDragExited(DragEvent event) {
		hide();
	}

	private void hide() {
		region.getChildren().remove(overlayRoot);
		overlayPane.setCenter(null);
	}

	private static Pane layout(List<ToggleButton> buttons, Pane region) {
		GridPane pane = new GridPane();
		pane.setVgap(10);

		if (buttons != null) {
			for (int i = 0; i < buttons.size(); i++) {
				ToggleButton button = buttons.get(i);
				button.setMaxHeight(Double.MAX_VALUE);
				button.setMaxWidth(Double.MAX_VALUE);
				button.setPrefHeight(region.getHeight() / buttons.size());
				button.setPrefWidth(region.getWidth());
				pane.add(button, 0, i);
			}
		} else {
			Rectangle rect = new Rectangle();
			pane.getChildren().add(rect);
		}

		pane.setMaxWidth(Double.MAX_VALUE);
		pane.setMaxHeight(Double.MAX_VALUE);

		return pane;
	}

}
