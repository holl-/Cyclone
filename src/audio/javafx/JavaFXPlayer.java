package audio.javafx;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import audio.AbstractPlayer;
import audio.AudioBuffer;
import audio.AudioDataFormat;
import audio.AudioDevice;
import audio.AudioEngine;
import audio.AudioEngineException;
import audio.MarkerEvent;
import audio.MarkerListener;
import audio.MediaFile;
import audio.MediaFormat;
import audio.MediaInfo;
import audio.MediaStream;
import audio.Player;
import audio.PlayerEvent;
import audio.UnsupportedMediaFormatException;

import javafx.beans.value.ChangeListener;
import javafx.scene.media.MediaMarkerEvent;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaPlayer.Status;
import javafx.util.Duration;

public class JavaFXPlayer extends AbstractPlayer {
	private AudioEngine engine;
	private JavaFXMedia media;

	private MediaPlayer fxPlayer;
	private CountDownLatch playerReady;
	private JavaFXMediaInfo info;
	private JavaFXBufferInfo buffer;

	private Map<String, Marker> markersByKey = new HashMap<String, Marker>();



	public JavaFXPlayer(AudioEngine e, JavaFXMedia m) {
		engine = e;
		media = m;
	}

	@Override
	public MediaFile getMediaFile() {
		return media.getMediaFile();
	}

	@Override
	public MediaStream getMediaStream() {
		return null;
	}

	@Override
	public synchronized void prepare() throws UnsupportedMediaFormatException, IOException {
		if(isPrepared()) return;

		media.prepare();

		fxPlayer = new MediaPlayer(media.getFXMedia());
		playerReady = new CountDownLatch(1);

		if(fxPlayer.getStatus() != Status.READY) {
			fxPlayer.setOnReady(() -> playerReady.countDown());
		} else {
			playerReady.countDown();
		}

		fxPlayer.setOnEndOfMedia(() -> fireEndOfMedia(getDuration()));
		fxPlayer.setOnMarker(e -> markerReached(e));
		fxPlayer.setOnError(() -> playerErrorOccurred());

		buffer = new JavaFXBufferInfo(fxPlayer);
	}

	@Override
	public void loadMediaFormat() throws IOException,
			UnsupportedMediaFormatException, UnsupportedOperationException {
		if(!isPrepared()) prepare();
		waitReady();

		info = new JavaFXMediaInfo(media.getFXMedia(), media.getMediaFile(), engine);
	}

	private void waitReady() {
		try {
			playerReady.await();
		} catch (InterruptedException e) {
			e.printStackTrace(); // cannot happen
		}
	}

	private void playerErrorOccurred() {
		if(!isPlaying()) {
			fireStopped(getPosition(), fxPlayer.getError());
		}
	}

	private void markerReached(MediaMarkerEvent e) {
		String key = e.getMarker().getKey();
		Marker marker = markersByKey.get(key);
		if(marker != null) {
			MarkerEvent markerEvent = new MarkerEvent(this, marker.position, true, false);
			marker.listener.markerPassed(markerEvent);
		}
	}

	@Override
	public synchronized boolean isPrepared() {
		if(media == null) return false;
		return media.isPrepared() && fxPlayer != null;
	}

	@Override
	public double estimatePreparationDuration()
			throws UnsupportedMediaFormatException, IOException {
		return 0.2;
	}

	@Override
	public MediaFormat getMediaFormat() {
		if(info != null) return info.getFormat();
		else return null;
	}

	@Override
	public MediaInfo getMediaInfo() {
		return info;
	}

	@Override
	public AudioDataFormat getEncodedFormat() {
		if(info != null) return info.getFormat().getAudioDataFormat();
		else return null;
	}

	@Override
	public AudioDataFormat getDecodedFormat() {
		return null;
	}

	@Override
	public MediaStream newEncodedStream() throws IOException,
			UnsupportedMediaFormatException, UnsupportedOperationException {
		throw new UnsupportedOperationException("not supported");
	}

	@Override
	public MediaStream newDecodedStream(double startPosition) throws IOException,
			UnsupportedMediaFormatException, IllegalStateException,
			UnsupportedOperationException {
		throw new UnsupportedOperationException("not supported");
	}

	@Override
	public boolean isActive() {
		return isPrepared();
	}

	@Override
	public void activate(AudioDevice device) throws AudioEngineException {
		if(device != JavaFXAudioEngine.DEVICE) throw new IllegalArgumentException("illegal device: "+device);
		fireActivated(getPosition(), PlayerEvent.USER_COMMAND);
	}

	@Override
	public void deactivate() {
		if(!isActive()) return;
		fxPlayer.stop();
		fireDeactivated(getPosition(), PlayerEvent.USER_COMMAND);
	}

	@Override
	public AudioDevice getDevice() {
		return JavaFXAudioEngine.DEVICE;
	}

	@Override
	public void switchDevice(AudioDevice device) throws AudioEngineException,
			IllegalStateException {
		if(device != JavaFXAudioEngine.DEVICE) throw new IllegalArgumentException("illegal device: "+device);
	}

	@Override
	public boolean canSetNext(Player next) throws IllegalStateException {
		if(!isPrepared()) throw new IllegalStateException("player must be prepared");
		return next == this || next == null;
	}

	@Override
	public void setNext(Player next) throws IllegalStateException,
			IllegalArgumentException {
		if(next == null) fxPlayer.setCycleCount(1);
		else if(next == this) fxPlayer.setCycleCount(MediaPlayer.INDEFINITE);
		else throw new IllegalArgumentException("only this player and null are supported");
	}

	@Override
	public Player getNext() {
		if(fxPlayer == null) return null;
		if(fxPlayer.getCycleCount() == MediaPlayer.INDEFINITE) return this;
		else return null;
	}

	@Override
	public AudioBuffer getAudioBuffer() {
		return buffer;
	}

	@Override
	public void dispose() {
		if(media == null) return;
		deactivate();
		fxPlayer.dispose();
		fxPlayer = null;
		media = null;
		info = null;
	}

	@Override
	public void start() throws IllegalStateException {
		if(fxPlayer == null) throw new IllegalStateException("player must be active to start");
		double position = getPosition();
		fxPlayer.play();
		fireStarted(position, PlayerEvent.USER_COMMAND);
	}

	@Override
	public void pause() {
		fxPlayer.pause();
		fireStopped(getPosition(), PlayerEvent.USER_COMMAND);
	}

	@Override
	public boolean isPlaying() {
		if(fxPlayer == null) return false;
		return fxPlayer.getStatus() == Status.PLAYING;
	}

	@Override
	public double getPosition() throws IllegalStateException {
		if(fxPlayer != null) {
			return fxPlayer.getCurrentTime().toSeconds();
		}
		else return -1;
	}



	@Override
	public void setPositionAsync(double position) {
		double oldPosition = getPosition();
		fxPlayer.seek(Duration.seconds(position));
		informMarkerListenersOnJump(oldPosition, position);
		firePositionChanged(oldPosition, PlayerEvent.USER_COMMAND);
	}

	@Override
	public void setPositionBlocking(double position, double timeout) throws InterruptedException {
		double oldPosition = getPosition();

		if(Math.abs(oldPosition - position) < 0.1) return;

		// Setup synchronization
		CountDownLatch posChangedLatch = new CountDownLatch(1);
		ChangeListener<Duration> listener = (posProp, oldValue, newValue) -> {
			double newPosition = newValue.toSeconds();
			if(!isPlaying() ||
					(newPosition-position >= -0.1 && newPosition - position <= 0.1)) {
				posChangedLatch.countDown();
			}
		};
		fxPlayer.currentTimeProperty().addListener(listener);

		// Seek and wait
		if(fxPlayer.getStatus() == Status.UNKNOWN) waitReady();
		if(fxPlayer.getStatus() == Status.STOPPED) fxPlayer.pause();

		// Sometimes the ChangeListener does not get notified
		if(fxPlayer.getStatus() == Status.READY) {
			long startTime = System.currentTimeMillis();
			new Thread(() -> {
				while(true) {
					try {
						Thread.sleep(50);
					} catch(InterruptedException exc){ return; }
					if(Math.abs(position - fxPlayer.getCurrentTime().toSeconds()) < 0.1){
						posChangedLatch.countDown();
						return;
					}
					if(System.currentTimeMillis()-startTime > timeout*1000) return;
				}
			}).start();
		}

		fxPlayer.seek(Duration.seconds(position));

		boolean positionSet = posChangedLatch.await((long) (timeout*1000), TimeUnit.MILLISECONDS);

		fxPlayer.currentTimeProperty().removeListener(listener);
		if(!positionSet) {
			double dif = Math.abs(position - fxPlayer.getCurrentTime().toSeconds());
			if(dif > 0.1) throw new InterruptedException("timeout, time="+fxPlayer.getCurrentTime().toSeconds()+", requested "+position);
		}
		informMarkerListenersOnJump(oldPosition, position);
		firePositionChanged(oldPosition, PlayerEvent.USER_COMMAND);
	}

	protected void informMarkerListenersOnJump(double oldPosition, double newPosition) {
		for(Marker marker : markersByKey.values()) {
			// Forward direction
			if(marker.position > oldPosition && marker.position <= newPosition) {
				MarkerEvent markerEvent = new MarkerEvent(this, marker.position, true, true);
				marker.listener.markerPassed(markerEvent);
			}
			// Backward direction
			else if(marker.position <= oldPosition && marker.position > newPosition) {
				MarkerEvent markerEvent = new MarkerEvent(this, marker.position, false, true);
				marker.listener.markerPassed(markerEvent);
			}
		}
	}

	@Override
	public double getDuration() {
//		return (int) fxMedia.getDuration().toSeconds();
		return fxPlayer.getStopTime().toSeconds();
	}



	@Override
	public double getGain() {
		if(fxPlayer == null) return 0;
		double volume = fxPlayer.getVolume();
		if(volume == 0) return Double.NEGATIVE_INFINITY;
		double dB = Math.log(volume) / Math.log(10.0) * 20.0;
		return dB;
	}

	@Override
	public void setGain(double dB) {
		if(fxPlayer != null) fxPlayer.setVolume(Math.pow(10, dB / 20));
		else throw new IllegalStateException("player is not prepared");
	}

	@Override
	public boolean isMute() {
		return fxPlayer.isMute();
	}

	@Override
	public void setMute(boolean mute) {
		if(fxPlayer != null) fxPlayer.setMute(mute);
		else throw new IllegalStateException("player is not prepared");
	}

	@Override
	public double getBalance() {
		if(fxPlayer != null) return fxPlayer.getBalance();
		else return 0;
	}

	@Override
	public void setBalance(double balance) throws IllegalStateException {
		if(fxPlayer != null) fxPlayer.setBalance(balance);
		else throw new IllegalStateException("player is not prepared");
	}



//	@Override
//	public void addMarker(double position, MarkerListener l) {
//		Marker marker = new Marker(position, l);
//		markersByKey.put(marker.key, marker);
//		media.getFXMedia().getMarkers().put(marker.key, Duration.seconds(position));
//	}
//
//	@Override
//	public void removeMarker(MarkerListener l) {
//		for(String key : markersByKey.keySet()) {
//			if(markersByKey.get(key).listener == l) {
//				markersByKey.remove(key);
//			}
//		}
//	}


	private static class Marker
	{
		private static int MARKER_INDEX = 0;

		public double position;
		public MarkerListener listener;
		public String key;

		public Marker(double position, MarkerListener l) {
			this.position = position;
			listener = l;
			key = "" + MARKER_INDEX++;
		}
	}


	@Override
	public void waitForDurationProperty()
			throws IllegalStateException, InterruptedException {
		if(!media.isPrepared()) throw new IllegalStateException("player is not prepared");
		media.waitForDurationKnown();
	}


}
