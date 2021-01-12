package player.fx.control;

import javafx.beans.binding.Bindings;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.*;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.util.converter.DoubleStringConverter;
import player.fx.icons.FXIcons;

import java.util.concurrent.Callable;

// TODO implement indeterminate (ProgressBar)
public class RoundPlayerSkin extends SkinBase<PlayerControl>
{
	private CircularSlider slider;

	// Buttons
	private Group buttonRoot;
	private ToggleButton playButton, loopButton, shuffleButton;
	private Button stopButton, prevButton, nextButton, listButton, searchButton;
	private final double innerMargin = 2, outerMargin = 5;
	private ImageView playIcon, pauseIcon;
	private final DoubleProperty heightScale = new SimpleDoubleProperty(1);
	private final double splitV = 0.2; // -1 to 1
	private final double splitH = 0.3;


	public RoundPlayerSkin(PlayerControl control) {
		super(control);
		createSlider();
		createButtons();
	}


	private void createSlider() {
		slider = new CircularSlider();
		// ToDo set ticks to 10/60 seconds
		slider.setMajorTickUnit(60);
		slider.setMinorTickCount(6);
		slider.setLabelFormatter(new DoubleStringConverter() {
			@Override
			public String toString(Double timeExact) {
				int time = (int) Math.round(timeExact);
				if(time < 60) {
					return time + " s";
				} else if(time < 60*60){
					int min = time / 60;
					int sec = time - min*60;
					return String.format("%d:%02d", min, sec);
				} else {
					int hrs = time / 60 / 60;
					int min = (time - hrs*60*60) / 60;
					int sec = time - hrs*60*60 - min*60;
					return String.format("%d:%02d:%02d", hrs, min, sec);
				}
			}
		});
		getChildren().add(slider);

		getSkinnable().durationProperty().addListener((e, o, duration) -> {
			if(duration.doubleValue() > 60 * 60) {
				slider.setMajorTickUnit(60 * 60);
				slider.setMinorTickCount(60);
			} else if(duration.doubleValue() > 30 * 60) {
				slider.setMajorTickUnit(60);
				slider.setMinorTickCount(0);
			} else {
				slider.setMajorTickUnit(60);
				slider.setMinorTickCount(6);
			}
		});

		slider.maxProperty().bind(getSkinnable().durationProperty());
		slider.valueProperty().bindBidirectional(getSkinnable().positionProperty());
		slider.disableProperty().bind(getSkinnable().mediaSelectedProperty().not());
	}


	private void createButtons() {
		BorderPane buttonPane = new BorderPane();
		buttonPane.setPickOnBounds(false);
		buttonRoot = new Group(buttonPane);
		getChildren().add(buttonRoot);

		playIcon = FXIcons.get("Play.png", 32, heightScale);
		pauseIcon = FXIcons.get("Pause.png", 32, heightScale);
		buttonPane.setCenter(playButton = new ToggleButton(null, null));
		playButton.selectedProperty().bindBidirectional(getSkinnable().playingProperty());
		playButton.graphicProperty().bind(Bindings.createObjectBinding(((Callable<Node>)() -> getSkinnable().isPlaying() ? pauseIcon : playIcon), getSkinnable().playingProperty()));

		BorderPane bottomButtonPane = new BorderPane();
		bottomButtonPane.setPickOnBounds(false);
		Group bottomRoot = new Group(bottomButtonPane);
		buttonPane.setBottom(bottomRoot);
		BorderPane.setAlignment(bottomRoot, Pos.TOP_CENTER);

		Insets buttonMargins = new Insets(innerMargin);
		bottomButtonPane.setCenter(stopButton = new Button(null, FXIcons.get("Stop.png", 20, heightScale)));
		bottomButtonPane.setLeft(prevButton = new Button(null, FXIcons.get("Previous.png", 32, heightScale)));
		BorderPane.setAlignment(prevButton, Pos.TOP_RIGHT);
		bottomButtonPane.setRight(nextButton = new Button(null, FXIcons.get("Next.png", 32, heightScale)));
		BorderPane.setMargin(playButton, buttonMargins);
		BorderPane.setMargin(stopButton, buttonMargins);
		BorderPane.setMargin(prevButton, buttonMargins);
		BorderPane.setMargin(nextButton, buttonMargins);
		playButton.setPickOnBounds(false);
		stopButton.setPickOnBounds(false);
		prevButton.setPickOnBounds(false);
		nextButton.setPickOnBounds(false);
		playButton.setTooltip(new Tooltip("Play / Pause"));
		stopButton.setTooltip(new Tooltip("Stop"));
		prevButton.setTooltip(new Tooltip("Previous song"));
		nextButton.setTooltip(new Tooltip("Next song"));
		playButton.disableProperty().bind(getSkinnable().mediaSelectedProperty().not());
		stopButton.disableProperty().bind(getSkinnable().mediaSelectedProperty().not());
		nextButton.disableProperty().bind(getSkinnable().playlistAvailableProperty().not());
		prevButton.disableProperty().bind(getSkinnable().playlistAvailableProperty().not());
		nextButton.setOnAction(e -> {
			if(getSkinnable().getOnNext() != null)
				getSkinnable().getOnNext().handle(e);
		});
		prevButton.setOnAction(e -> {
			if(getSkinnable().getOnPrevious() != null)
				getSkinnable().getOnPrevious().handle(e);
		});
		stopButton.setOnAction(e -> {
			if(getSkinnable().getOnStop() != null)
				getSkinnable().getOnStop().handle(e);
		});
		for(Node button : new Node[]{playButton, stopButton, nextButton, prevButton}) {
			button.setOnMousePressed(e -> {
				Node control = getNode();
				control.fireEvent(e.copyFor(control, control));
			});
			button.setOnMouseDragged(e -> {
				Node control = getNode();
				control.fireEvent(e.copyFor(control, control));
			});
		}

		// Loop
		getChildren().add(loopButton = new ToggleButton(null, FXIcons.get("Loop.png", 24, heightScale)));
		loopButton.selectedProperty().bindBidirectional(getSkinnable().loopProperty());
		loopButton.setTooltip(new Tooltip("Loop playlist"));

		// Shuffle
		getChildren().add(shuffleButton = new ToggleButton(null, FXIcons.get("Shuffle.png", 24, heightScale)));
		shuffleButton.selectedProperty().bindBidirectional(getSkinnable().shuffledProperty());
		shuffleButton.setTooltip(new Tooltip("Shuffled playlist\n"
				+ "When active: modifying playlist will trigger playlist to be shuffled\n"
				+ "On activation: shuffle playlist"));

		// Playlist
		getChildren().add(listButton = new Button(null, FXIcons.get("Playlist.png", 24, heightScale)));
		listButton.setOnAction(e -> {
			if(getSkinnable().getOnShowPlaylist() != null)
				getSkinnable().getOnShowPlaylist().handle(e);
		});
		listButton.setTooltip(new Tooltip("Show playlist"));

		// Search
		getChildren().add(searchButton = new Button(null, FXIcons.get("Search.png", 24, heightScale)));
		searchButton.setOnAction(e -> {
			if(getSkinnable().getOnSearch() != null)
				getSkinnable().getOnSearch().handle(e);
		});
		searchButton.setTooltip(new Tooltip("Search for file or folder"));
	}

	private void layoutButtons(double rad) {
		double inset = innerMargin;
		Circle region = new Circle(rad);
		setButtonShape(playButton, Shape.intersect(region, new Rectangle(-rad, -rad, 2*rad, (1+splitV)*rad-inset)));
		setButtonShape(stopButton, Shape.intersect(region, new Rectangle(-rad*splitH+inset, splitV*rad+inset, 2*splitH*rad-2*inset, (1-splitV)*rad-inset)));
		setButtonShape(prevButton, Shape.intersect(region, new Rectangle(-rad, splitV*rad+inset, (1-splitH)*rad-inset, (1-splitV)*rad-inset)));
		setButtonShape(nextButton, Shape.intersect(region, new Rectangle(rad*splitH+inset, splitV*rad+inset, (1-splitH)*rad-inset, (1-splitV)*rad-inset)));
	}

	private static void setButtonShape(ButtonBase button, Shape shape) {
		Bounds b = shape.getBoundsInLocal();
		button.setShape(shape);
		button.setMinSize(b.getWidth(), b.getHeight());
		button.setMaxSize(b.getWidth(), b.getHeight());
	}

		@Override
	    protected double computeMinWidth(double height,
	            double topInset, double rightInset,
	            double bottomInset, double leftInset) {
	        return height;
	    }

		@Override
		protected double computeMinHeight(double width, double topInset, double rightInset, double bottomInset,
				double leftInset) {
			return width;
		}

    @Override
    protected void layoutChildren(double contentX, double contentY,
            double contentWidth, double contentHeight) {
    	double barRadius = Math.min(contentWidth, contentHeight) * slider.getDiameter() / 2;
    	double barWidth = Math.min(contentWidth, contentHeight) * slider.getThickness();

    	heightScale.set(barRadius / 120);

		layoutButtons((barRadius-barWidth/2) - outerMargin);
    	layoutInArea(slider, contentX, contentY, contentWidth, contentHeight, 0, HPos.CENTER, VPos.CENTER);
    	layoutInArea(buttonRoot, contentX, contentY, contentWidth, contentHeight, 0, HPos.CENTER, VPos.CENTER);

    	double inset = 10;
    	layoutInArea(loopButton, contentX+inset, contentY+inset, contentWidth-2*inset, contentHeight-2*inset, 0, HPos.LEFT, VPos.BOTTOM);
    	layoutInArea(shuffleButton, contentX+inset, contentY+inset, contentWidth-2*inset, contentHeight-2*inset, 0, HPos.RIGHT, VPos.BOTTOM);
    	layoutInArea(listButton, contentX+inset, contentY+inset, contentWidth-2*inset, contentHeight-2*inset, 0, HPos.LEFT, VPos.TOP);
    	layoutInArea(searchButton, contentX+inset, contentY+inset, contentWidth-2*inset, contentHeight-2*inset, 0, HPos.RIGHT, VPos.TOP);
    }

}