package com.mp3player.playback;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.mp3player.audio2.javasound.JavaSoundEngine;
import com.mp3player.desktopaudio.AudioDevice;
import com.mp3player.desktopaudio.AudioEngine;
import com.mp3player.desktopaudio.AudioEngineException;
import com.mp3player.desktopaudio.LocalMediaFile;
import com.mp3player.desktopaudio.MediaFile;
import com.mp3player.desktopaudio.Player;
import com.mp3player.model.Identifier;
import com.mp3player.player.status.MachineInfo;
import com.mp3player.player.status.PlaybackStatus;
import com.mp3player.player.status.PlayerStatus;
import com.mp3player.player.status.PlayerTarget;
import com.mp3player.player.status.Speaker;
import com.mp3player.vdp.RemoteFile;

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


	public PlaybackEngine(PlayerStatus status) throws AudioEngineException {
		this.status = status;
		target = status.getTarget();
		info = status.getPlayback();

		audio = new JavaSoundEngine();
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



	private void targetChanged() {
		Optional<AudioDevice> localDevice = findLocalDevice(target.getTargetDevice());
		if(localDevice.isPresent()) {
			loadFile();
			if(player != null) adjustPlayer(localDevice.get());
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

			if(currentFile.isPresent()) {
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
	}






	private void adjustPlayer(AudioDevice localDevice) {
		if(mute != target.isTargetMute()) {
			mute = target.isTargetMute();
			player.setMute(mute);
		}
		if(gain != target.getTargetGain()) {
			gain = target.getTargetGain();
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

	public void publishInfo() {
		boolean playing = player != null ? player.isPlaying() : false;
		double position = player != null ? player.getPosition() : 0;
		double duration = player != null ? player.getDuration() : 0;

		info.setStatus(target.getTargetDevice(), supportedTypes, currentMediaID,
				gain, mute,
				playing, false, errorMessage, position, System.currentTimeMillis(), duration);
	}


	public Optional<AudioDevice> findLocalDevice(Optional<Speaker> device) {
		if(!device.isPresent()) return Optional.empty();
		String id = device.get().getId();
		return audio.getDevices().stream().filter(dev -> dev.getID().equals(id)).findAny();
	}

}
