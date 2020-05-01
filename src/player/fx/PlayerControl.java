package player.fx;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.BooleanPropertyBase;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.DoublePropertyBase;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.AccessibleAttribute;
import javafx.scene.control.Control;
import javafx.scene.control.Skin;

public class PlayerControl extends Control {
	private DoubleProperty duration = new DoublePropertyBase(0) {
        @Override protected void invalidated() {
            notifyAccessibleAttributeChanged(AccessibleAttribute.VALUE);
        }

        @Override
        public Object getBean() {
            return PlayerControl.this;
        }

        @Override
        public String getName() {
            return "duration";
        }
    };
	public double getDuration() { return durationProperty().get(); }
	public void setDuration(double duration) { durationProperty().set(duration); }
	public DoubleProperty durationProperty() { return duration; }



	private DoubleProperty position = new DoublePropertyBase(0) {
        @Override protected void invalidated() {
            notifyAccessibleAttributeChanged(AccessibleAttribute.MAX_VALUE);
        }

        @Override
        public Object getBean() {
            return PlayerControl.this;
        }

        @Override
        public String getName() {
            return "position";
        }
    };
	public final DoubleProperty positionProperty() { return position; }
	public double getPosition() { return positionProperty().get(); }
	public void setPosition(double position) { positionProperty().set(position); }


	/** true if minor tick marks should be displayed */
    private BooleanProperty loop = new BooleanPropertyBase(true) {
        @Override
        public Object getBean() {
            return PlayerControl.this;
        }

        @Override
        public String getName() {
            return "loop";
        }
    };
    public final boolean isLoop() { return loop.get(); }
    public final void setLoop(boolean value) { loop.set(value); }
    public final BooleanProperty loopProperty() { return loop; }

    /** true if minor tick marks should be displayed */
    private BooleanProperty shuffled = new BooleanPropertyBase(false) {
        @Override
        public Object getBean() {
            return PlayerControl.this;
        }

        @Override
        public String getName() {
            return "shuffled";
        }
    };
    public final boolean isShuffled() { return shuffled.get(); }
    public final void setShuffled(boolean value) { shuffled.set(value); }
    public final BooleanProperty shuffledProperty() { return shuffled; }

    /** true if minor tick marks should be displayed */
    private BooleanProperty playing = new BooleanPropertyBase(false) {
        @Override
        public Object getBean() {
            return PlayerControl.this;
        }

        @Override
        public String getName() {
            return "playing";
        }
    };
    public final boolean isPlaying() { return playing.get(); }
    public final void setPlaying(boolean value) { playing.set(value); }
    public final BooleanProperty playingProperty() { return playing; }

    private BooleanProperty mediaSelected = new BooleanPropertyBase(false) {
        @Override
        public Object getBean() {
            return PlayerControl.this;
        }

        @Override
        public String getName() {
            return "mediaSelected";
        }
    };
    public final boolean isMediaSelected() { return mediaSelected.get(); }
    public final void setMediaSelected(boolean value) { mediaSelected.set(value); }
    public final BooleanProperty mediaSelectedProperty() { return mediaSelected; }

    private BooleanProperty playlistAvailable = new BooleanPropertyBase(false) {
        @Override
        public Object getBean() {
            return PlayerControl.this;
        }

        @Override
        public String getName() {
            return "playlistAvailable";
        }
    };
    public final boolean isPlaylistAvailable() { return playlistAvailable.get(); }
    public final void setPlaylistAvailable(boolean value) { playlistAvailable.set(value); }
    public final BooleanProperty playlistAvailableProperty() { return playlistAvailable; }

    private ObjectProperty<EventHandler<ActionEvent>> onNext = new SimpleObjectProperty<>();
    public EventHandler<ActionEvent> getOnNext() { return onNext.get(); }
    public void setOnNext(EventHandler<ActionEvent> value) { onNext.setValue(value); }
    public ObjectProperty<EventHandler<ActionEvent>> onNextProperty() { return onNext; }

    private ObjectProperty<EventHandler<ActionEvent>> onPrevious = new SimpleObjectProperty<>();
    public EventHandler<ActionEvent> getOnPrevious() { return onPrevious.get(); }
    public void setOnPrevious(EventHandler<ActionEvent> value) { onPrevious.setValue(value); }
    public ObjectProperty<EventHandler<ActionEvent>> onPreviousProperty() { return onPrevious; }

    private ObjectProperty<EventHandler<ActionEvent>> onStop = new SimpleObjectProperty<>();
    public EventHandler<ActionEvent> getOnStop() { return onStop.get(); }
    public void setOnStop(EventHandler<ActionEvent> value) { onStop.setValue(value); }
    public ObjectProperty<EventHandler<ActionEvent>> onStopProperty() { return onStop; }

    private ObjectProperty<EventHandler<ActionEvent>> onShowPlaylist = new SimpleObjectProperty<>();
    public EventHandler<ActionEvent> getOnShowPlaylist() { return onShowPlaylist.get(); }
    public void setOnShowPlaylist(EventHandler<ActionEvent> value) { onShowPlaylist.setValue(value); }
    public ObjectProperty<EventHandler<ActionEvent>> onShowPlaylistProperty() { return onShowPlaylist; }

    private ObjectProperty<EventHandler<ActionEvent>> onSearch = new SimpleObjectProperty<>();
    public EventHandler<ActionEvent> getOnSearch() { return onSearch.get(); }
    public void setOnSearch(EventHandler<ActionEvent> value) { onSearch.setValue(value); }
    public ObjectProperty<EventHandler<ActionEvent>> onSearchProperty() { return onSearch; }


	public PlayerControl() {
		getStyleClass().add("time-slider");
	}


	@Override
	protected Skin<?> createDefaultSkin() {
		return new RoundPlayerSkin(this);
	}
}