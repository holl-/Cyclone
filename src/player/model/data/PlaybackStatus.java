package player.model.data;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import distributed.DFile;
import distributed.Conflict;
import distributed.Distributed;

/**
 * This shared data object reflects the current status of playback. It is only
 * manipulated by the playback engine and contains properties that may be
 * displayed by the application.
 *
 * @author Philipp Holl
 *
 */
public class PlaybackStatus extends Distributed {
	private static final long serialVersionUID = -5405377670442949541L;

	private Optional<Speaker> device = Optional.empty();
	private List<String> supportedFormats = Collections.emptyList();

	private Optional<DFile> currentMedia = Optional.empty(); // media ID

	private double gain;
	private boolean mute;

	private boolean playing;
	private boolean busy;
	private String busyText; // or error text when not playing & not busy
	private boolean endOfMediaReached;

	private double lastKnownPosition; // in seconds
	private long lastUpdateTime; // in milliseconds
	private double duration;

	public PlaybackStatus() {
		super(false, false);
	}

	public void setStatus(Optional<Speaker> device, List<String> supportedFormats, Optional<DFile> currentMedia, double gain,
			boolean mute, boolean playing, boolean busy, String busyText,
			double lastKnownPosition, long lastUpdateTime, double duration, boolean endOfMediaReached) {
		if(currentMedia == null) throw new IllegalArgumentException("currentMedia = null");
		if(supportedFormats == null) throw new IllegalArgumentException("supportedFormats = null");
		this.device = device;
		this.supportedFormats = supportedFormats;
		this.currentMedia = currentMedia;
		this.gain = gain;
		this.mute = mute;
		this.playing = playing;
		this.busy = busy;
		this.busyText = busyText;
		this.lastKnownPosition = lastKnownPosition;
		this.lastUpdateTime = lastUpdateTime;
		this.duration = duration;
		this.endOfMediaReached = endOfMediaReached;
		fireChangedLocally();
	}

	public void updatePosition(double lastKnownPosition, long lastUpdateTime) {
		this.lastKnownPosition = lastKnownPosition;
		this.lastUpdateTime = lastUpdateTime;
		fireChangedLocally();
	}

	public double getCurrentPosition() {
		if (!playing)
			return lastKnownPosition;
		return lastKnownPosition + (System.currentTimeMillis() - lastUpdateTime) / 1e3;
	}

	public Optional<Speaker> getDevice() {
		return device;
	}

	public Optional<DFile> getCurrentMedia() {
		return currentMedia;
	}

	public List<String> getSupportedFormats() {
		return supportedFormats;
	}

	public double getGain() {
		return gain;
	}

	public boolean isMute() {
		return mute;
	}

	public boolean isPlaying() {
		return playing;
	}

	public boolean isBusy() {
		return busy;
	}

	public String getBusyText() {
		return busyText;
	}

	public double getLastKnownPosition() {
		return lastKnownPosition;
	}

	public long getLastUpdateTime() {
		return lastUpdateTime;
	}

	public double getDuration() {
		return duration;
	}

	public boolean getEndOfMediaReached() {
		return endOfMediaReached;
	}

}
