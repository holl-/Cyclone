package audio.javafx;

import audio.*;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class JavaFXAudioEngine extends AudioEngine
{
	private List<MediaType> supportedFormats;

	private List<Player> players = new CopyOnWriteArrayList<Player>();
	private Map<MediaFile, JavaFXMedia> mediaMap = new HashMap<MediaFile, JavaFXMedia>();


	public JavaFXAudioEngine() {
		super("JavaFX");

		// see https://docs.oracle.com/javafx/2/media/overview.htm
		supportedFormats = Arrays.asList(new MediaType("MP3", "mp3"),
				new MediaType("AIFF", "aif"),
				new MediaType("WAVE", "wav"));
	}

	@Override
	public Player newPlayer(MediaFile media) {
		JavaFXMedia fxMedia;
		if(mediaMap.containsKey(media)) {
			fxMedia = mediaMap.get(media);
		}
		else {
			fxMedia = new JavaFXMedia(media);
			mediaMap.put(media, fxMedia);
		}

		Player player = new JavaFXPlayer(this, fxMedia);
		players.add(player);
		return player;
	}

	@Override
	public Player newPlayer(MediaStream stream) {
		throw new UnsupportedOperationException("streaming not supported");
	}

	@Override
	public List<Player> getPlayers() {
		return Collections.unmodifiableList(players);
	}

	@Override
	public Collection<MediaType> getSupportedMediaTypes() {
		return supportedFormats;
	}

	@Override
	public AudioDevice getDefaultDevice() {
		return DEVICE;
	}

	@Override
	public List<AudioDevice> getDevices() {
		return Arrays.asList(DEVICE);
	}


	@Override
	public void dispose() {
		logger.fine("Disposing of AudioEngine");
		for(Player player : players) {
			player.dispose();
		}
	}

	@Override
	public boolean isStreamingSupported() {
		return false;
	}


	public static final AudioDevice DEVICE = new AudioDevice() {

		@Override
		public boolean isDefault() {
			return true;
		}

		@Override
		public String getName() {
			return "Primary device";
		}

		@Override
		public double getMinGain() {
			return Double.NEGATIVE_INFINITY;
		}

		@Override
		public double getMaxGain() {
			return 0;
		}

		@Override
		public int getMaxActivePlayers() {
			return -1;
		}

		@Override
		public String getID() {
			return "FX_DEVICE";
		}
	};
}
