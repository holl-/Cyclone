package player.playback;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import audio.javafx.JavaFXAudioEngine;
import audio.javasound.JavaSoundEngine;
import audio.AudioDevice;
import audio.AudioEngine;
import audio.AudioEngineException;
import audio.LocalMediaFile;
import audio.MediaFile;
import audio.Player;
import player.model.Identifier;
import vdp.RemoteFile;

/**
 * Controls the local audio engine.
 * An instance of PlaybackEngine observe a PlayerStatus.
 * Whenever an audio file should be played back on a local device, the PlaybackEngine loads the file and plays it back.
 * It also adjusts position and gain to match the desired status.
 *
 * When a playback event occurs like an error or end-of-file, the PlaybackEngine updates the status.
 */
public class PlaybackEngine {
	private PlayerStatus status;
	private PlayerTarget target;
	private PlaybackStatus info;
	private AudioEngine audio;
	private final List<String> supportedTypes;

	private Optional<RemoteFile> currentFile = Optional.empty(); // locally playing media
	private Optional<Identifier> currentMediaID = Optional.empty();
	private Player player;
	private long lastPositionUpdate;

	private double gain;
	private boolean mute;
	private String errorMessage;


	private PlaybackEngine(PlayerStatus status, AudioEngine audioEngine) throws AudioEngineException {
		this.status = status;
		target = status.getTarget();
		info = status.getPlayback();

		audio = audioEngine;
		supportedTypes = new ArrayList<>(audio.getSupportedMediaTypes().stream().map(t -> t.getFileExtension()).collect(Collectors.toList()));

		// Publish audio devices
		String peerID = status.getVdp().getLocalPeer().getID();
		List<Speaker> speakers = audio.getDevices().stream()
				.map(dev -> new Speaker(peerID, dev.getID(), dev.getName(), dev.getMinGain(), dev.getMaxGain(), dev.isDefault()))
				.collect(Collectors.toList());
		status.getVdp().putData(new MachineInfo(status.getVdp().getLocalPeer(), speakers));

		// Set device if not present
		if(!target.getTargetDevice().isPresent()) {
			target.setTargetDevice(Optional.of(speakers.get(0)));
		}

		status.getTarget().addDataChangeListener(e -> targetChanged());
	}


	public static PlaybackEngine initializeAudioEngine(PlayerStatus status, String engineName) throws AudioEngineException {
		AudioEngine engine;
		if(engineName == null) engine = new JavaSoundEngine();
		else if(engineName.equals("java")) engine = new JavaSoundEngine();
		else if(engineName.equals("javafx")) engine = new JavaFXAudioEngine();
		else throw new AudioEngineException("No audio engine registered for name " + engineName);
		return new PlaybackEngine(status, engine);
	}


	private void targetChanged() {
		if(isTarget()) {
			loadFile();
			gain = target.getTargetGain();
			mute = target.isTargetMute();
			if(player != null) adjustPlayer();
			publishInfo();
		}
		else if(!audio.getPlayers().isEmpty()){
			audio.getPlayers().forEach(player -> player.dispose());
		}
	}


	private void loadFile() {
		if(!currentMediaID.equals(target.getTargetMedia())) {
			// Change file

			if(player != null) player.dispose();
			player = null;

			publishInfo();

			currentMediaID = target.getTargetMedia();
			currentFile = Identifier.lookup(currentMediaID, status.getVdp());
			if(!currentFile.isPresent()) return;

            MediaFile file;
            if(currentFile.get().getPeer().isLocal()) {
                file = new LocalMediaFile(currentFile.get().localFile());
            } else {
                // TODO copy to local
                throw new UnsupportedOperationException("file copying not supported yet");
            }
            player = audio.newPlayer(file);
            try {
                player.prepare();
                player.activate(audio.getDefaultDevice());
                player.setMute(mute);
                player.setGain(gain);
                player.addEndOfMediaListener(e -> status.next());
                if(player.getDuration() < 0) {
                    new Thread(() -> {
                        try {
                            player.waitForDurationProperty();
                        } catch (IllegalStateException e1) {
                            e1.printStackTrace();
                            return;
                        } catch (InterruptedException e1) {
                            return;
                        }
                        publishInfo();
                    }).start();
                }
                errorMessage = null;
            } catch(Exception exc) {
                player = null;
                exc.printStackTrace();
                errorMessage = exc.getMessage();
            }
		}
	}


	private void adjustPlayer() {
		AudioDevice localDevice = findLocalDevice(target.getTargetDevice()).get();
		if(mute != player.isMute()) {
			player.setMute(mute);
		}
		if(gain != player.getGain()) {
			player.setGain(gain);
		}
		if(target.wasTargetPositionSetAfter(lastPositionUpdate)) {
			lastPositionUpdate = target.getPositionUpdateTime();
			try {
				player.setPositionBlocking(target.getTargetPosition().getAsDouble(), 1.0);
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
		if(player.getDevice() != localDevice) {
			try {
				player.switchDevice(localDevice);
			} catch (IllegalStateException | AudioEngineException e) {
				errorMessage = e.getMessage();
			}
		}

		if(target.isTargetPlaying()) {
			player.start();
		} else {
			player.pause();
		}
	}

    /**
     * Updates the PlaybackStatus to reflect the current status of the audio engine.
     * This may result in the information being sent to connected machines.
     */
	private void publishInfo() {
		boolean playing = player != null && player.isPlaying();
		double position = player != null ? player.getPosition() : 0;
		double duration = player != null ? player.getDuration() : 0;

		info.setStatus(target.getTargetDevice(), supportedTypes, currentMediaID,
				gain, mute,
				playing, false, errorMessage, position, System.currentTimeMillis(), duration);
	}


	private Optional<AudioDevice> findLocalDevice(Optional<Speaker> speaker) {
		if(!speaker.isPresent()) return Optional.empty();
		String id = speaker.get().getId();
		return audio.getDevices().stream().filter(dev -> dev.getID().equals(id)).findAny();
	}

	private boolean isTarget() {
		return findLocalDevice(target.getTargetDevice()).isPresent();
	}


	public Player currentPlayer() {
	    return player;
    }

}
