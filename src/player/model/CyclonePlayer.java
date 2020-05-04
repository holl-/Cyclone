package player.model;

import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import player.model.data.*;
import distributed.*;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.*;
import java.util.stream.Collectors;

public class CyclonePlayer {
	private CycloneConfig config;
	private MediaLibrary library;
	private String controllerId = UUID.randomUUID().toString();

	// Data objects
	private PlaybackStatus playbackData;
	private PlayerTarget targetData;
	private Playlist playlistData;
	private SpeakerList devicesData;

	private String noMediaText = "No media selected.";

	// Public properties

	private ReadOnlyDoubleProperty duration;
	public double getDuration() { return duration.get(); }
	public ReadOnlyDoubleProperty durationProperty() { return duration; }

	private DistributedDoubleProperty position;
	public double getPosition() { return position.get(); }
	public void setPosition(double value) { position.set(value); }
	public DoubleProperty positionProperty() { return position; }

	private BooleanProperty playing;
	public boolean isPlaying() { return playing.get(); }
	public void setPlaying(boolean value) { playing.set(value); }
	public BooleanProperty playingProperty() { return playing; }

	private ReadOnlyStringProperty title;
	public String getTitle() { return title.get(); }
	public ReadOnlyStringProperty titleProperty() { return title; }

	private ReadOnlyBooleanProperty mediaSelected;
	public boolean isMediaSelected() { return mediaSelected.get(); }
	public ReadOnlyBooleanProperty mediaSelectedProperty() { return mediaSelected; }

	private ReadOnlyBooleanProperty playlistAvailable;
	public boolean isPlaylistAvailable() { return playlistAvailable.get(); }
	public ReadOnlyBooleanProperty playlistAvailableProperty() { return playlistAvailable; }

	private DistributedDoubleProperty gain;
	public double getGain() { return gain.get(); }
	public void setGain(double value) { gain.set(value); }
	public DoubleProperty gainProperty() { return gain; }

	private BooleanProperty mute;
	public boolean isMute() { return mute.get(); }
	public void setMute(boolean value) { mute.set(value); }
	public BooleanProperty muteProperty() { return mute; }

	private BooleanProperty loop;
	public boolean isLoop() { return loop.get(); }
	public void setLoop(boolean value) { loop.set(value); }
	public BooleanProperty loopProperty() { return loop; }

	private BooleanProperty shuffled;
	public boolean isShuffled() { return shuffled.get(); }
	public void setShuffled(boolean value) { shuffled.set(value); }
	public BooleanProperty shuffledProperty() { return shuffled; }

	private ObservableList<DFile> playlist;
	public ObservableList<DFile> getPlaylist() { return playlist; }

	private ObjectProperty<Optional<DFile>> currentMedia;
	public Optional<DFile> getCurrentMedia() { return currentMedia.get(); }
	public void setCurrentMedia(Optional<DFile> value) { currentMedia.set(value); }
	public ObjectProperty<Optional<DFile>> currentMediaProperty() { return currentMedia; }

	private ObservableList<Speaker> speakers;
	public ObservableList<Speaker> getSpeakers() { return speakers; }

	private ObjectProperty<Optional<Speaker>> speaker;
	public Optional<Speaker> getSpeaker() { return speaker.get(); }
	public void setSpeaker(Optional<Speaker> value) { speaker.set(value); }
	public ObjectProperty<Optional<Speaker>> speakerProperty() { return speaker; }



	public CyclonePlayer(CycloneConfig config) {
		this.config = config;
		library = new MediaLibrary();
		if(config.getProperties().containsKey("library")) {
			String[] roots = config.getString("library", "").split(";");
			for(String root : roots) {
				root = root.trim();
				if(!root.isEmpty())
					library.getRoots().add(new DFile(new File(root)));
			}
		} else library.addDefaultRoots();
		// Create data objects
		playbackData = new PlaybackStatus();
		playlistData = new Playlist();
		targetData = new PlayerTarget();
		devicesData = new SpeakerList();

		playbackData.addDataChangeListener(e -> {
			playbackData.getCurrentMedia().ifPresent(m -> this.library.getRecentlyUsed().add(0, m));
			if(playbackData.getEndOfMediaReached()) {
				if(controllerId.equals(targetData.getProgramControllerId()))
					next();
			}
		});

		playing = new DistributedBooleanProperty("playing", this,
				playbackData,
				() -> playbackData.isPlaying(),
				newValue -> {
					if(!playbackData.getCurrentMedia().isPresent() && !playlistData.isEmpty()) next();
					else targetData.setTargetPlaying(newValue);
					});

		duration = new DistributedDoubleProperty("duration", this,
				playbackData,
				() -> playbackData.getDuration(),
				newValue -> { throw new UnsupportedOperationException("cannot set duration"); });

		title = new DistributedReadOnlyStringProperty("title", this,
				playbackData,
				() -> {
					if(playbackData.getBusyText() != null) return playbackData.getBusyText();
					return playbackData.getCurrentMedia().map(m -> new File(m.getPath()).getName()).orElse(noMediaText);
					});

		mediaSelected = new DistributedBooleanProperty("mediaSelected", this,
				playbackData,
				() -> playbackData.getCurrentMedia() != null && playbackData.getCurrentMedia().isPresent(),
				newValue -> targetData.setTargetPlaying(newValue));

		playlistAvailable = new DistributedBooleanProperty("playlistAvailable", this,
				playlistData, playbackData,
				() -> playlistData.size() > 1 || (playlistData.size() == 1 && !playbackData.getCurrentMedia().isPresent()),
				newValue -> { throw new UnsupportedOperationException("cannot set duration"); });

		position = new DistributedDoubleProperty("position", this,
				playbackData,
				() -> playbackData.getCurrentPosition(),
				newValue -> {
					if(newValue >= 0) targetData.setTargetPosition(newValue, true);
				}) {
			@Override
			public double get() {
				return playbackData.getCurrentPosition();
			}
		};
		Executors.newScheduledThreadPool(1).scheduleAtFixedRate(() -> {
			if(isPlaying()) {
				Platform.runLater(() -> position.invalidated());
			}
		}, 50, 50, TimeUnit.MILLISECONDS);

		gain = new DistributedDoubleProperty("gain", this,
				playbackData,
				() -> playbackData.getGain(),
				newValue -> targetData.setTargetGain(newValue));

		mute = new DistributedBooleanProperty("mute", this,
				playbackData,
				() -> playbackData.isMute(),
				newValue -> targetData.setTargetMute(newValue));

		loop = new DistributedBooleanProperty("loop", this,
				targetData,
				() -> targetData.isLoop(),
				newValue -> targetData.setLoop(newValue));

		shuffled = new DistributedBooleanProperty("shuffled", this,
				targetData,
				() -> targetData.isShuffled(),
				newValue -> {
					targetData.setShuffled(newValue);
					playlistData.shuffle(playbackData.getCurrentMedia());
				});

		playlist = FXCollections.observableArrayList();
		playlistData.addDataChangeListener(e -> {
			Platform.runLater(() -> playlist.setAll(playlistData.list()));
		});

		currentMedia = new DistributedObjectProperty<>("currentMedia", this,
				playbackData,
				() -> playbackData.getCurrentMedia(),
				newValue -> targetData.setTargetMedia(newValue, true, controllerId));

		speakers = FXCollections.observableArrayList(devicesData.getSpeakers());
		devicesData.addDataChangeListener(e -> {
			List<Speaker> availableSpeakers = devicesData.getSpeakers().stream().filter(Distributed::exists).collect(Collectors.toList());
			Platform.runLater(() -> speakers.setAll(availableSpeakers));
		});

		speaker = new DistributedObjectProperty<>("speaker", this,
				playbackData,
				() -> playbackData.getDevice(),
				newValue -> targetData.setTargetDevice(newValue));
	}


	public List<Distributed> getDistributedObjects() {
		return Arrays.asList(targetData, playbackData, playlistData, devicesData);
	}

	public SpeakerList getDevicesData() {
		return devicesData;
	}


	public PlaybackStatus getPlaybackStatus() {
		return playbackData;
	}

	public PlayerTarget getPlayerTarget() {
		return targetData;
	}

	public MediaLibrary getLibrary() {
		return library;
	}

	public void stop() {
		targetData.stop();
	}

	public DFile addToPlaylist(List<DFile> files, int returnIndex) {
		return playlistData.addAll(files, returnIndex, isShuffled(), getCurrentMedia());
	}

	public DFile setPlaylist(List<DFile> files, int returnIndex, boolean firstStayFirst) {
		return playlistData.setAll(files, returnIndex, isShuffled(), firstStayFirst);
	}


	public Optional<DFile> getNext() {
		return playlistData.getNext(playbackData.getCurrentMedia(), targetData.isLoop());
	}
	public void next() {
		targetData.setTargetMedia(getNext(), true, controllerId);
	}

	public Optional<DFile> getPrevious() {
		return playlistData.getPrevious(playbackData.getCurrentMedia(), targetData.isLoop());
	}
	public void previous() {
		targetData.setTargetMedia(getPrevious(), true, controllerId);
	}


	private static class DistributedDoubleProperty extends DoubleProperty
	{
		private String name;
		private Object bean;

		private Distributed distributed;
		private DoubleSupplier getter;
		private DoubleConsumer setter;
		private double lastValue;

		private List<ChangeListener<? super Number>> changeListeners = new CopyOnWriteArrayList<>();
		private List<InvalidationListener> invalidationListeners = new CopyOnWriteArrayList<>();


		public DistributedDoubleProperty(String name, Object bean, Distributed distributed, DoubleSupplier getter,
				DoubleConsumer setter) {
			this.name = name;
			this.bean = bean;
			this.distributed = distributed;
			this.getter = getter;
			this.setter = setter;
			invalidated();
			register();
		}

		private void register() {
			distributed.addDataChangeListener(e -> Platform.runLater(() -> invalidated()));
		}

		protected void invalidated() {
			double newValue = getter.getAsDouble();
			if(newValue == lastValue) return;
			double oldValue = lastValue;
			lastValue = newValue;
			fireChangedInvalidated(oldValue, newValue);
		}

		protected void fireChangedInvalidated(double oldValue, double newValue) {
			for(ChangeListener<? super Number> l : changeListeners) {
				l.changed(this, oldValue, newValue);
			}
			for(InvalidationListener l : invalidationListeners) {
				l.invalidated(this);
			}
		}


		@Override
		public void bind(ObservableValue<? extends Number> observable) {
			throw new UnsupportedOperationException();
		}
		@Override
		public void unbind() {
			return;
		}
		@Override
		public boolean isBound() {
			return false;
		}
		@Override
		public Object getBean() {
			return bean;
		}
		@Override
		public String getName() {
			return name;
		}
		@Override
		public void addListener(ChangeListener<? super Number> listener) {
			changeListeners.add(listener);
		}
		@Override
		public void removeListener(ChangeListener<? super Number> listener) {
			changeListeners.remove(listener);
		}
		@Override
		public void addListener(InvalidationListener listener) {
			invalidationListeners.add(listener);
		}
		@Override
		public void removeListener(InvalidationListener listener) {
			invalidationListeners.remove(listener);
		}
		@Override
		public double get() {
			return lastValue;
		}
		@Override
		public void set(double value) {
			if(value != lastValue) setter.accept(value);
		}
	}


	private static class DistributedBooleanProperty extends BooleanProperty
	{
		private String name;
		private Object bean;

		private BooleanSupplier getter;
		private Consumer<Boolean> setter;
		private boolean lastValue;

		private List<ChangeListener<? super Boolean>> changeListeners = new CopyOnWriteArrayList<>();
		private List<InvalidationListener> invalidationListeners = new CopyOnWriteArrayList<>();


		public DistributedBooleanProperty(String name, Object bean, Distributed distributed, BooleanSupplier getter,
				Consumer<Boolean> setter) {
			this.name = name;
			this.bean = bean;
			this.getter = getter;
			this.setter = setter;
			invalidated();
			register(distributed);
		}

		public DistributedBooleanProperty(String name, Object bean, Distributed distributed1, Distributed distributed2, BooleanSupplier getter,
				Consumer<Boolean> setter) {
			this.name = name;
			this.bean = bean;
			this.getter = getter;
			this.setter = setter;
			invalidated();
			register(distributed1);
			register(distributed2);
		}

		private void register(Distributed distributed) {
			distributed.addDataChangeListener(e -> Platform.runLater(() -> invalidated()));
		}

		protected void invalidated() {
			boolean newValue = getter.getAsBoolean();
			if(newValue == lastValue) return;
			boolean oldValue = lastValue;
			lastValue = newValue;
			fireChangedInvalidated(oldValue, newValue);
		}

		protected void fireChangedInvalidated(boolean oldValue, boolean newValue) {
			for(ChangeListener<? super Boolean> l : changeListeners) {
				l.changed(this, oldValue, newValue);
			}
			for(InvalidationListener l : invalidationListeners) {
				l.invalidated(this);
			}
		}


		@Override
		public void bind(ObservableValue<? extends Boolean> observable) {
			throw new UnsupportedOperationException();
		}
		@Override
		public void unbind() {
			return;
		}
		@Override
		public boolean isBound() {
			return false;
		}
		@Override
		public Object getBean() {
			return bean;
		}
		@Override
		public String getName() {
			return name;
		}
		@Override
		public void addListener(ChangeListener<? super Boolean> listener) {
			changeListeners.add(listener);
		}
		@Override
		public void removeListener(ChangeListener<? super Boolean> listener) {
			changeListeners.remove(listener);
		}
		@Override
		public void addListener(InvalidationListener listener) {
			invalidationListeners.add(listener);
		}
		@Override
		public void removeListener(InvalidationListener listener) {
			invalidationListeners.remove(listener);
		}
		@Override
		public boolean get() {
			return lastValue;
		}
		@Override
		public void set(boolean value) {
			if(value != lastValue) setter.accept(value);
		}
	}


	private static class DistributedReadOnlyStringProperty extends ReadOnlyStringProperty
	{
		private String name;
		private Object bean;

		private Distributed distributed;
		private Supplier<String> getter;
		private String lastValue;

		private List<ChangeListener<? super String>> changeListeners = new CopyOnWriteArrayList<>();
		private List<InvalidationListener> invalidationListeners = new CopyOnWriteArrayList<>();


		public DistributedReadOnlyStringProperty(String name, Object bean, Distributed distributed, Supplier<String> getter) {
			this.name = name;
			this.bean = bean;
			this.distributed = distributed;
			this.getter = getter;
			invalidated();
			register();
		}

		private void register() {
			distributed.addDataChangeListener(e -> Platform.runLater(() -> invalidated()));
		}

		protected void invalidated() {
			String newValue = getter.get();
			if(newValue == lastValue) return;
			String oldValue = lastValue;
			lastValue = newValue;
			fireChangedInvalidated(oldValue, newValue);
		}

		protected void fireChangedInvalidated(String oldValue, String newValue) {
			for(ChangeListener<? super String> l : changeListeners) {
				l.changed(this, oldValue, newValue);
			}
			for(InvalidationListener l : invalidationListeners) {
				l.invalidated(this);
			}
		}
		@Override
		public Object getBean() {
			return bean;
		}
		@Override
		public String getName() {
			return name;
		}
		@Override
		public void addListener(ChangeListener<? super String> listener) {
			changeListeners.add(listener);
		}
		@Override
		public void removeListener(ChangeListener<? super String> listener) {
			changeListeners.remove(listener);
		}
		@Override
		public void addListener(InvalidationListener listener) {
			invalidationListeners.add(listener);
		}
		@Override
		public void removeListener(InvalidationListener listener) {
			invalidationListeners.remove(listener);
		}
		@Override
		public String get() {
			return lastValue;
		}
	}

	private static class DistributedObjectProperty<T> extends ObjectProperty<T>
	{
		private String name;
		private Object bean;

		private Supplier<T> getter;
		private Consumer<T> setter;
		private T lastValue;

		private List<ChangeListener<? super T>> changeListeners = new CopyOnWriteArrayList<>();
		private List<InvalidationListener> invalidationListeners = new CopyOnWriteArrayList<>();


		public DistributedObjectProperty(String name, Object bean, Distributed distributed, Supplier<T> getter,
				Consumer<T> setter) {
			this.name = name;
			this.bean = bean;
			this.getter = getter;
			this.setter = setter;
			invalidated();
			register(distributed);
		}

		private void register(Distributed distributed) {
			distributed.addDataChangeListener(e -> Platform.runLater(() -> invalidated()));
		}

		protected void invalidated() {
			T newValue = getter.get();
			if(newValue.equals(lastValue)) return;
			T oldValue = lastValue;
			lastValue = newValue;
			fireChangedInvalidated(oldValue, newValue);
		}

		protected void fireChangedInvalidated(T oldValue, T newValue) {
			for(ChangeListener<? super T> l : changeListeners) {
				l.changed(this, oldValue, newValue);
			}
			for(InvalidationListener l : invalidationListeners) {
				l.invalidated(this);
			}
		}


		@Override
		public void bind(ObservableValue<? extends T> observable) {
			throw new UnsupportedOperationException();
		}
		@Override
		public void unbind() {
			return;
		}
		@Override
		public boolean isBound() {
			return false;
		}
		@Override
		public Object getBean() {
			return bean;
		}
		@Override
		public String getName() {
			return name;
		}
		@Override
		public void addListener(ChangeListener<? super T> listener) {
			changeListeners.add(listener);
		}
		@Override
		public void removeListener(ChangeListener<? super T> listener) {
			changeListeners.remove(listener);
		}
		@Override
		public void addListener(InvalidationListener listener) {
			invalidationListeners.add(listener);
		}
		@Override
		public void removeListener(InvalidationListener listener) {
			invalidationListeners.remove(listener);
		}
		@Override
		public T get() {
			return lastValue;
		}
		@Override
		public void set(T value) {
			if(!lastValue.equals(value)) setter.accept(value);
		}
	}
}
