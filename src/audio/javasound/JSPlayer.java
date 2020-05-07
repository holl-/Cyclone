package audio.javasound;

import java.io.IOException;

import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

import audio.javasound.lib.JavaSoundMixer;
import audio.javasound.lib.MemoryAudioBuffer;
import audio.AbstractPlayer;
import audio.AudioDataFormat;
import audio.AudioDevice;
import audio.AudioEngineException;
import audio.MarkerListener;
import audio.MediaFile;
import audio.MediaFormat;
import audio.MediaInfo;
import audio.MediaStream;
import audio.Player;
import audio.PlayerEvent;
import audio.UnsupportedMediaFormatException;

public class JSPlayer extends AbstractPlayer
{
	private JavaSoundEngine engine;
	private Media media;

	// Target
	private JSChannel channel; // active when not null
	private double gain;
	private boolean mute;
	private double balance;
	private int offlinePositionMillis; // position in millis

	// Listeners, etc.
	private Player next;



	public JSPlayer(Media m) {
		media = m;
		engine = m.getEngine();
		m.addUser(this);
	}


	@Override
	public MediaFile getMediaFile() {
		return media.getMediaFile();
	}

	@Override
	public MediaStream getMediaStream() {
		return media.getStream();
	}

	@Override
	public void prepare() throws UnsupportedMediaFormatException,
			IOException {
		media.prepare();
	}


	@Override
	public MediaStream newEncodedStream() throws IOException,
			UnsupportedMediaFormatException {
		return media.newEncodedStream();
	}

	@Override
	public MediaStream newDecodedStream(double startPosition) throws IOException,
			UnsupportedMediaFormatException {
		return media.newDecodedStream((int) (startPosition*1000));
	}

	@Override
	public boolean isPrepared() {
		return media.isPrepared();
	}

	@Override
	public double estimatePreparationDuration()
			throws UnsupportedMediaFormatException, IOException {
		return media.estimatePreparationDuration();
	}

	@Override
	public MediaFormat getMediaFormat() {
		return media.getMediaFormat();
	}

	/**
	 * Always loads encodedAudioFormat
	 * and info if supported
	 */
	@Override
	public void loadMediaFormat() throws IOException,
			UnsupportedMediaFormatException, UnsupportedOperationException
	{
		media.loadMediaFormat();
	}

	@Override
	public MediaInfo getMediaInfo() {
		return media.getInfo();
	}

	@Override
	public boolean isActive() {
		return channel != null;
	}


	@Override
	public void activate(AudioDevice device) throws AudioEngineException {
		if(!isPrepared()) throw new IllegalStateException("player must be prepared first");
		if(isActive()) throw new IllegalStateException("player is already active");
		if(media.getEncodedAudioFormat() == null) throw new IllegalStateException("player must be prepared before activating");
		if(!(device instanceof JavaSoundMixer)) throw new IllegalArgumentException("illegal device: "+device);

		try {
			channel = new JSChannel(media.getBuffer().getFormat());
		} catch (UnsupportedAudioFileException e) {
			throw new AudioEngineException(e);
		}
		channel.setPlayer(this);
		try {
			channel.setDevice((JavaSoundMixer) device);
		} catch (LineUnavailableException e) {
			throw new AudioEngineException(e);
		}

		fireActivated(offlinePositionMillis, PlayerEvent.USER_COMMAND);
	}

	@Override
	public void deactivate() {
		if(!isActive()) return;
		if(isPlaying()) pause();
		channel.dispose();
		channel = null;

		fireDeactivated(offlinePositionMillis, PlayerEvent.USER_COMMAND);
	}

	@Override
	public MemoryAudioBuffer getAudioBuffer() {
		return media.getBuffer();
	}

	@Override
	public void dispose() {
		deactivate();
		engine.remove(this);
		media.removeUserDealloc(this);
	}

	@Override
	public boolean isPlaying() {
		if(!isActive()) return false;
		return channel.isRunning();
	}

	@Override
	public void start() throws IllegalStateException {
		if(!isActive()) throw new IllegalStateException("must be active to start");
		channel.start();

		fireStarted(offlinePositionMillis, PlayerEvent.USER_COMMAND);
	}

	@Override
	public void pause() {
		if(!isActive()) throw new IllegalStateException("player is not active");
		channel.pause();
		offlinePositionMillis = channel.getPositionMillis();

		fireStopped(offlinePositionMillis, PlayerEvent.USER_COMMAND);
	}

	@Override
	public double getPosition() throws IllegalStateException {
		if(!isActive()) return offlinePositionMillis / 1000.0;
		return channel.getPositionMillis() / 1000.0;
	}

	@Override
	public void setPositionBlocking(double posSec, double timeout) {
		if(posSec < 0) throw new IllegalArgumentException("pos < 0");
		int posMillis = (int) (posSec * 1000);
		double oldPosition = getPosition();

		offlinePositionMillis = posMillis;
		if(isActive()) {
			channel.seek(posMillis);
		}

		firePositionChanged(oldPosition, PlayerEvent.USER_COMMAND);
	}


	@Override
	public void setPositionAsync(double position, Runnable onFinished) {
		new Thread(() -> {
			setPositionBlocking(position, -1);
			if(onFinished != null) onFinished.run();
		}).start();
	}

	@Override
	public double getDuration() {
		return media.getDuration();
	}

	@Override
	public double getGain() {
		return gain;
	}

	@Override
	public void setGain(double dB) {
		gain = dB;
		if(channel != null) {
			channel.updateGain();
		}
	}

	@Override
	public boolean isMute() {
		return mute;
	}

	@Override
	public void setMute(boolean m) {
		mute = m;
		if(channel != null) {
			channel.updateMute();
		}
	}


	@Override
	public double getBalance() {
		return balance;
	}


	@Override
	public void setBalance(double bal) throws IllegalStateException {
		balance = bal;
		if(channel != null) {
			channel.setBalance(balance);
		}
	}

	@Override
	public AudioDevice getDevice() {
		if(channel == null) return null;
		return channel.getDevice();
	}

	@Override
	public void switchDevice(AudioDevice device) throws AudioEngineException {
		if(channel == null) throw new IllegalStateException("must be active to switch device");
		if(!(device instanceof JavaSoundMixer)) throw new IllegalArgumentException("illegal device: "+device);
		try {
			channel.setDevice((JavaSoundMixer) device);
		} catch (LineUnavailableException e) {
			throw new AudioEngineException(e);
		}
	}

	@Override
	public boolean canSetNext(Player next) throws IllegalStateException
	{
		if(!(next instanceof JSPlayer)) return false;
		JSPlayer nextJS = (JSPlayer) next;
		return channel.matches(nextJS);
	}

	@Override
	public void setNext(Player nextPlayer) throws IllegalStateException,
			IllegalArgumentException {
		next = nextPlayer;
		throw new UnsupportedOperationException("not yet implemented");
	}

	@Override
	public Player getNext() {
		return next;
	}

	@Override
	public AudioDataFormat getEncodedFormat() {
		return media.getEncodedAudioFormat();
	}

	@Override
	public AudioDataFormat getDecodedFormat() {
		return media.getDecodedAudioFormat();
	}

	public int getInactivePosition() {
		return offlinePositionMillis;
	}


	public void streamEnded(int positionMillis) {
		fireEndOfMedia(positionMillis);
	}


	@Override
	public void waitForDurationProperty() throws IllegalStateException,
			InterruptedException {
		if(!isPrepared()) throw new IllegalStateException("not prepared");
		media.waitUntilBufferFilled();
	}
}
