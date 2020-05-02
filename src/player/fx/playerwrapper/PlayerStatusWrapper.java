package player.fx.playerwrapper;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import player.model.Identifier;
import player.model.MediaIndex;
import player.model.MediaInfo;
import player.playback.MachineInfo;
import player.playback.PlayerStatus;
import player.playback.Speaker;
import vdp.DataEvent;
import vdp.DataListener;
import vdp.Distributed;
import vdp.VDP;

import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class PlayerStatusWrapper {
	private PlayerStatus status;
	private MediaIndex index;

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

	private ObservableList<MediaInfo> playlist;
	public ObservableList<MediaInfo> getPlaylist() { return playlist; }

	private ObjectProperty<Optional<Identifier>> currentMedia;
	public Optional<Identifier> getCurrentMedia() { return currentMedia.get(); }
	public void setCurrentMedia(Optional<Identifier> value) { currentMedia.set(value); }
	public ObjectProperty<Optional<Identifier>> currentMediaProperty() { return currentMedia; }

	private ObservableList<Speaker> speakers;
	public ObservableList<Speaker> getSpeakers() { return speakers; }

	private ObjectProperty<Optional<Speaker>> speaker;
	public Optional<Speaker> getSpeaker() { return speaker.get(); }
	public void setSpeaker(Optional<Speaker> value) { speaker.set(value); }
	public ObjectProperty<Optional<Speaker>> speakerProperty() { return speaker; }



	public PlayerStatusWrapper(PlayerStatus status, MediaIndex index) {
		this.status = status;
		this.index = index;

		playing = new DistributedBooleanProperty("playing", this,
				status.getPlayback(),
				() -> status.getPlayback().isPlaying(),
				newValue -> {
					if(!status.getPlayback().getCurrentMedia().isPresent() && !status.getPlaylist().isEmpty()) status.next();
					else status.getTarget().setTargetPlaying(newValue);
					});

		duration = new DistributedDoubleProperty("duration", this,
				status.getPlayback(),
				() -> status.getPlayback().getDuration(),
				newValue -> { throw new UnsupportedOperationException("cannot set duration"); });

		title = new DistributedReadOnlyStringProperty("title", this,
				status.getPlayback(),
				() -> {
					if(status.getPlayback().getBusyText() != null) return status.getPlayback().getBusyText();
					return status.getPlayback().getCurrentMedia().map(m -> new File(m.getPath()).getName()).orElse(noMediaText);
					});

		mediaSelected = new DistributedBooleanProperty("mediaSelected", this,
				status.getPlayback(),
				() -> status.getPlayback().getCurrentMedia() != null && status.getPlayback().getCurrentMedia().isPresent(),
				newValue -> status.getTarget().setTargetPlaying(newValue));

		playlistAvailable = new DistributedBooleanProperty("playlistAvailable", this,
				status.getPlaylist(), status.getPlayback(),
				() -> status.getPlaylist().size() > 1 || (status.getPlaylist().size() == 1 && !status.getPlayback().getCurrentMedia().isPresent()),
				newValue -> { throw new UnsupportedOperationException("cannot set duration"); });

		position = new DistributedDoubleProperty("position", this,
				status.getPlayback(),
				() -> status.getPlayback().getCurrentPosition(),
				newValue -> {
					if(newValue >= 0) status.getTarget().setTargetPosition(newValue, true);
				}) {
			@Override
			public double get() {
				return status.getPlayback().getCurrentPosition();
			}
		};
		Executors.newScheduledThreadPool(1).scheduleAtFixedRate(() -> {
			if(isPlaying()) {
				Platform.runLater(() -> position.invalidated());
			}
		}, 50, 50, TimeUnit.MILLISECONDS);

		gain = new DistributedDoubleProperty("gain", this,
				status.getPlayback(),
				() -> status.getPlayback().getGain(),
				newValue -> status.getTarget().setTargetGain(newValue));

		mute = new DistributedBooleanProperty("mute", this,
				status.getPlayback(),
				() -> status.getPlayback().isMute(),
				newValue -> status.getTarget().setTargetMute(newValue));

		loop = new DistributedBooleanProperty("loop", this,
				status.getTarget(),
				() -> status.getTarget().isLoop(),
				newValue -> status.getTarget().setLoop(newValue));

		shuffled = new DistributedBooleanProperty("shuffled", this,
				status.getTarget(),
				() -> status.getTarget().isShuffled(),
				newValue -> {
					status.getTarget().setShuffled(newValue);
					status.getPlaylist().shuffle(status.getPlayback().getCurrentMedia());
				});

		playlist = FXCollections.observableArrayList();
		status.getPlaylist().addDataChangeListener(e -> {
			Platform.runLater(() -> playlist.setAll(status.getPlaylist().list().stream()
					.map(id -> index.getInfo(id))
					.filter(opMed -> opMed.isPresent())
					.map(opMed -> opMed.get())
					.collect(Collectors.toList())));
		});

		currentMedia = new DistributedObjectProperty<>("currentMedia", this,
				status.getPlayback(),
				() -> status.getPlayback().getCurrentMedia(),
				newValue -> status.getTarget().setTargetMedia(newValue, true));

		speakers = FXCollections.observableArrayList();
		status.getVdp().addDataListener(new DataListener() {
			@Override
			public void onDataRemoved(DataEvent e) {
				// TODO Auto-generated method stub

			}
			@Override
			public void onDataChanged(DataEvent e) {
				// TODO Auto-generated method stub

			}
			@Override
			public void onDataAdded(DataEvent e) {
				if(MachineInfo.getPeer(e.getData()).isPresent()) {
					MachineInfo info = (MachineInfo)e.getData();
					speakers.addAll(info.getSpeakers());
				}
			}
		});

		speaker = new DistributedObjectProperty<>("speaker", this,
				status.getPlayback(),
				() -> status.getPlayback().getDevice(),
				newValue -> status.getTarget().setTargetDevice(newValue));
	}


	public PlayerStatus getStatus() {
		return status;
	}

	public MediaIndex getIndex() {
		return index;
	}

	public void stop() {
		status.getTarget().stop();
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

	public VDP getVdp() {
		return status.getVdp();
	}
}
