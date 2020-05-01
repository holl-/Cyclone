package player.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalLong;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import vdp.RemoteFile;

public class MediaInfo implements Serializable {
	private static final long serialVersionUID = -4199985097804825767L;

	public enum Property {
		/** String */
		TITLE,
		/** Double, duration in seconds */
		DURATION,
		/** String */
		ALBUM,
		/** Integer */
		TRACK_NUMBER,
		/** String */
		AUTHOR,
		/** String */
		DATE,
		/** String or Boolean */
		COPYRIGHT,
		/** String */
		COMMENT,
		/** String */
		TRACK,
		/** String */
		GENRE,
		/** String, multichannel mode, e.g. "Joint Stereo" or "5.1" */
		MODE,

		/** Long */
		LAST_USED
	}

	private Identifier media;

	private String machinePath;
	private long modificationDate;
	private boolean directory;

	private Map<Property, Serializable> properties;
	private Map<String, Serializable> customProperties;

	private transient List<Consumer<MediaInfoEvent>> changeListeners = new CopyOnWriteArrayList<>();

	public MediaInfo(RemoteFile file) {
		media = new Identifier(file);
		machinePath = file.getAbsolutePath();
		modificationDate = file.lastModified();
		directory = file.isDirectory();

		properties = new HashMap<>();
		customProperties = new HashMap<>();
	}

	public boolean isDirectory() {
		return directory;
	}

	public Identifier getIdentifier() {
		return media;
	}

	public String getMachinePath() {
		return machinePath;
	}

	public long getModificationDate() {
		return modificationDate;
	}

	public OptionalDouble getDuration() {
		return getDouble(Property.DURATION);
	}

	public void setDuration(long duration) {
		set(Property.DURATION, duration);
	}

	public Optional<String> getTitle() {
		return get(Property.TITLE);
	}

	public void setTitle(String title) {
		set(Property.TITLE, title);
	}

	public Optional<String> getAlbumt() {
		return get(Property.ALBUM);
	}

	public void setAlbum(String album) {
		set(Property.ALBUM, album);
	}

	public OptionalLong getLastUsed() {
		return getLong(Property.LAST_USED);
	}

	public void setLastUsed(long lastUsed) {
		set(Property.LAST_USED, lastUsed);
	}

	private void set(Property property, Serializable value) {
		properties.put(property, value);
		MediaInfoEvent e = new MediaInfoEvent(this, property, null, value);
		changeListeners.forEach(l -> l.accept(e));
	}

	public void setCustomProperty(String customProperty, Serializable value) {
		customProperties.put(customProperty, value);
		MediaInfoEvent e = new MediaInfoEvent(this, null, customProperty, value);
		changeListeners.forEach(l -> l.accept(e));
	}

	@SuppressWarnings("unchecked")
	public <T> Optional<T> getCustomProperty(String customProperty) {
		Object value = customProperties.get(customProperty);
		if (value == null)
			return Optional.empty();
		else
			return Optional.of((T) value);
	}

	public Map<String, ? extends Object> getCustomProperties() {
		return customProperties;
	}

	private OptionalLong getLong(Property property) {
		Long value = (Long) properties.get(property);
		if (value == null)
			return OptionalLong.empty();
		else
			return OptionalLong.of(value);
	}

	private OptionalDouble getDouble(Property property) {
		Double value = (Double) properties.get(property);
		if (value == null)
			return OptionalDouble.empty();
		else
			return OptionalDouble.of(value);
	}

	private <T> Optional<T> get(Property property) {
		@SuppressWarnings("unchecked")
		T value = (T) properties.get(property);
		return Optional.ofNullable(value);
	}

	public String getRelativePath() {
		return media.getPath();
	}

	public String getDisplayTitle() {
		return getTitle().orElse(media.inferTitle());
	}

	@Override
	public String toString() {
		return media.toString();
	}
}
