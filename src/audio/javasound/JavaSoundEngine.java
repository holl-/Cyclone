package audio.javasound;

import audio.*;
import audio.javasound.lib.AudioSystem2;
import audio.javasound.lib.DefaultMediaInfo;
import audio.javasound.lib.JavaSoundMixer;
import audio.javasound.lib.MP3Info;

import javax.sound.sampled.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;

public class JavaSoundEngine extends AudioEngine
{
	private List<JavaSoundMixer> devices;
	private JavaSoundMixer defaultDevice;

	private List<MediaType> supportedTypes;

	private List<Player> players = new CopyOnWriteArrayList<Player>();
	private Map<MediaFile, Media> mediaMap = new HashMap<MediaFile, Media>();


	public JavaSoundEngine() throws AudioEngineException {
		super("Java Sound");

		devices = getOutputDevices(SourceDataLine.class);
		for(JavaSoundMixer device : devices) {
			if(device.isDefault()) {
				defaultDevice = device;
				break;
			}
		}

		supportedTypes = new ArrayList<MediaType>(AudioSystem2.SUPPORTED_TYPES.length);
		for(AudioFileFormat.Type type : AudioSystem2.SUPPORTED_TYPES) {
			supportedTypes.add(new MediaType(type.toString(), type.getExtension()));
		}
	}



	@Override
	public JSPlayer newPlayer(MediaFile media) {
		Media prep;

		if(mediaMap.containsKey(media)) {
			prep = mediaMap.get(media);
		} else {
			prep = new Media(this, media);
			mediaMap.put(media, prep);
		}

		JSPlayer player =  new JSPlayer(prep);
		players.add(player);
		return player;
	}

	@Override
	public JSPlayer newPlayer(MediaStream stream) {
		Media prep = new Media(this, stream);
		JSPlayer player =  new JSPlayer(prep);
		players.add(player);
		return player;
	}

	@Override
	public List<Player> getPlayers() {
		return Collections.unmodifiableList(players);
	}

	public MediaInfo createMediaInfo(MediaFile media, MediaFormat format) {
		if(media.getFileName().toLowerCase().endsWith(".mp3")) {
			return new MP3Info(format);
		}
		else {
			return new DefaultMediaInfo(media, format);
		}
	}

	public MediaInfo createMediaInfo(MediaStream stream) {
		MediaFormat format = stream.getMediaFormat();
		if(format == null) return null;
		if(!format.matchesAudioEngine(this)) throw new IllegalArgumentException("format not compatible with this AudioEngine");

		boolean isMP3 = format.getType().getFileExtension().toLowerCase().equals("mp3");
		if(isMP3) {
			return new MP3Info(format);
		}
		else {
			return new DefaultMediaInfo(stream, format);
		}
	}

	@Override
	public boolean isStreamingSupported() {
		return true;
	}


	@Override
	public void dispose() {
		logger.fine("Disposing of AudioEngine");
		for(Player player : players) {
			player.dispose();
		}
	}


	public void errorOccurred(LineUnavailableException e, String msg) {
		logger.log(Level.WARNING, msg, e);
	}





	public List<JavaSoundMixer> getOutputDevices(Class<?> sourceLineClass) {
		List<JavaSoundMixer> supportedList = new ArrayList<JavaSoundMixer>();

		Mixer.Info[] mixerInfos = AudioSystem.getMixerInfo();

		Mixer defaultMixer = AudioSystem.getMixer(null);

		boolean defaultMixerFound = false;

		for(int i = 0; i < mixerInfos.length; i++) {
			Mixer.Info mixerInfo = mixerInfos[i];
			Mixer mixer = AudioSystem.getMixer(mixerInfo);
			boolean supported = false;

			// Mixer Inputs
			for(Line.Info mixerInputInfo : mixer.getSourceLineInfo()){
				if(mixerInputInfo.getLineClass() == sourceLineClass) {
					supported = true;
					break;
				}
			}

			if(supported) {
				boolean isDefault = mixer == defaultMixer; // alternatively i == 0
				supportedList.add(new JavaSoundMixer(this, mixer, isDefault));
				defaultMixerFound = true;
			}
		}

		if(!defaultMixerFound && !supportedList.isEmpty()) {
			supportedList.get(0).setDefault(true);
		}

		return supportedList;
	}


	@Override
	public List<AudioDevice> getDevices() {
		return Collections.unmodifiableList(devices);
	}

	@Override
	public AudioDevice getDefaultDevice() {
		if(defaultDevice == null) throw new IllegalStateException("must be initialized first");
		return defaultDevice;
	}


	public void remove(JSPlayer jsPlayer) {
		players.remove(jsPlayer);
	}


	@Override
	public Collection<MediaType> getSupportedMediaTypes() {
		return supportedTypes;
	}


}
