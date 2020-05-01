package com.mp3player.desktopaudio;

import java.io.Serializable;
import java.util.HashMap;

/*
 * Originally SAudioFileFormat
 */

/**
 * MediaFormat stores the properties of a media.
 * The properties available depend on the {@link AudioEngine} which was used
 * to load the format.
 * 
 * <p>It's equivalent in the Java Sound API is <code>AudioFileFormat</code>.
 * However, <code>MediaFormat</code> is not necessarily limited to audio format
 * information.
 * </p>
 * 
 * @author Philipp Holl
 *
 */
public class MediaFormat implements Serializable {
	private static final long serialVersionUID = -3870624281460365852L;
	
	private String audioEngineName;
	
	private MediaType type;
	private int frameLength;
	private HashMap<String, Object> properties;
	private AudioDataFormat audioDataFormat;
	
	
	public MediaFormat(AudioEngine audioEngine, MediaType type, int frameLength,
			HashMap<String, Object> properties, AudioDataFormat audioDataFormat) {
		this.audioEngineName = audioEngine.getName();
		this.type = type;
		this.frameLength = frameLength;
		this.properties = properties;
		this.audioDataFormat = audioDataFormat;
	}

	
	
	public String getAudioEngineName() {
		return audioEngineName;
	}
	
	public boolean matchesAudioEngine(AudioEngine e) {
		if(e == null) return false;
		return e.getName().equals(audioEngineName);
	}

	


	public MediaType getType() {
		return type;
	}



	public int getFrameLength() {
		return frameLength;
	}


	public HashMap<String, Object> getProperties() {
		return properties;
	}

	public Object getProperty(String key) {
		if(properties == null) return null;
		return properties.get(key);
	}


	public AudioDataFormat getAudioDataFormat() {
		return audioDataFormat;
	}



	@Override
	public String toString() {
		return "MediaFormat [audioEngineName=" + audioEngineName + ", type="
				+ type + ", frameLength=" + frameLength + "]";
	}

	
	
}
