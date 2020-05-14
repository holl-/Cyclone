package audio.javasound;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

import audio.javasound.lib.JavaSoundMixer;
import audio.javasound.lib.VirtualChannel;

public class JSChannel
{
	private JavaSoundMixer mixer;
	private VirtualChannel vChannel;

	private JSPlayer activePlayer;
	private int frameOffset; // start frame of the currently playing stream


	public JSChannel(AudioFormat f) throws UnsupportedAudioFileException {
		vChannel = new VirtualChannel(f);
		vChannel.setOnPlaybackEnded(() -> streamEnded());
	}

	private void streamEnded() {
		int positionMillis = (int) (vChannel.getReadFrames() / vChannel.getFormat().getFrameRate() * 1000.0);
		activePlayer.streamEnded(positionMillis);
	}

	public void setPlayer(JSPlayer player) {
		activePlayer = player;
		seek(player.getInactivePosition());
	}

	public void setDevice(JavaSoundMixer device, double bufferTime) throws LineUnavailableException {
		if(mixer == device) return;
		mixer = device;
		vChannel.setLine(mixer.getMixer(), activePlayer.getGain(), activePlayer.isMute(), true, bufferTime);
	}

	public void seek(int posMillis) {
		if(posMillis < 0) throw new IllegalArgumentException("pos < 0");
		int startFrame = activePlayer.getAudioBuffer().getFrame(posMillis);
		AudioInputStream stream = activePlayer.getAudioBuffer().audioStreamFromFrame(startFrame);
		vChannel.setInputStream(stream, true, true);
		frameOffset = startFrame;
	}

	public int getPositionMillis() {
		int read = (int) (vChannel.getReadFrames() - vChannel.getFrameLag());
		if(read < 0) read = 0;
		int framePosition = frameOffset + read;
		return (int) (1000.0 * framePosition / vChannel.getFormat().getFrameRate());
	}

	public void start() {
		vChannel.start();
	}

	public boolean isRunning() {
		return vChannel.isRunning();
	}

	public void pause() {
		vChannel.stop();
	}

	public JavaSoundMixer getDevice() {
		return mixer;
	}

	public void dispose() {
		vChannel.dispose(true, true);
	}

	public void updateGain() {
		vChannel.setGain(activePlayer.getGain());
	}

	public void updateMute() {
		vChannel.setMute(activePlayer.isMute());
	}

	public boolean matches(JSPlayer nextJS) {
		if(nextJS.getDecodedFormat() == null) throw new IllegalStateException("decoded format unknown");
		return nextJS.getDecodedFormat().equalFormat(activePlayer.getDecodedFormat());
	}

	public double getBalance() {
		return vChannel.getBalance();
	}

	public void setBalance(double balance) {
		vChannel.setBalance(balance);
	}


}
