package audio;

import java.io.Serializable;
import java.util.HashMap;

/*
 * Originally SAudioFormat
 */
/**
 * <code>AudioDataFormat</code> contains information about the format
 * of <i>decoded</i> audio data.
 * 
 * <p>It's equivalent in the Java Sound API is <code>AudioFormat</code>.
 * </p>
 * @author Philipp Holl
 *
 */
public class AudioDataFormat implements Serializable {
	private static final long serialVersionUID = -720357510839438095L;

	
	private String encodingName;
	private String encodingClassName;
	private float sampleRate;
	private int sampleSizeInBits;
	private int channels;
	private int frameSize;
	private float frameRate;
	private boolean bigEndian;
	private HashMap<String, Object> properties;
	
	
	public AudioDataFormat(String encodingName, String encodingClassName,
			float sampleRate, int sampleSizeInBits, int channels,
			int frameSize, float frameRate, boolean bigEndian,
			HashMap<String, Object> properties) {
		this.encodingName = encodingName;
		this.encodingClassName = encodingClassName;
		this.sampleRate = sampleRate;
		this.sampleSizeInBits = sampleSizeInBits;
		this.channels = channels;
		this.frameSize = frameSize;
		this.frameRate = frameRate;
		this.bigEndian = bigEndian;
		this.properties = properties;
	}


	public String getEncodingName() {
		return encodingName;
	}


	public String getEncodingClassName() {
		return encodingClassName;
	}


	public float getSampleRate() {
		return sampleRate;
	}


	public int getSampleSizeInBits() {
		return sampleSizeInBits;
	}


	public int getChannels() {
		return channels;
	}


	public int getFrameSize() {
		return frameSize;
	}


	public float getFrameRate() {
		return frameRate;
	}


	public boolean isBigEndian() {
		return bigEndian;
	}


	public HashMap<String, Object> getProperties() {
		return properties;
	}

	public Object getProperty(String key) {
		if(properties == null) return null;
		return properties.get(key);
	}


	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (bigEndian ? 1231 : 1237);
		result = prime * result + channels;
		result = prime
				* result
				+ ((encodingClassName == null) ? 0 : encodingClassName
						.hashCode());
		result = prime * result
				+ ((encodingName == null) ? 0 : encodingName.hashCode());
		result = prime * result + Float.floatToIntBits(frameRate);
		result = prime * result + frameSize;
		result = prime * result + Float.floatToIntBits(sampleRate);
		result = prime * result + sampleSizeInBits;
		return result;
	}


	
	/**
	 * This method does not compare the properties and not the
	 * encodingClassName.
	 * @param other a format to compare this one to
	 * @return true if the formats are equal in terms of audio data format
	 */
	public boolean equalFormat(AudioDataFormat other) {
		if (this == other) return true;
		if (other == null) return false;
		
		if (bigEndian != other.bigEndian)
			return false;
		if (channels != other.channels)
			return false;
		if (encodingName == null) {
			if (other.encodingName != null)
				return false;
		} else if (!encodingName.equals(other.encodingName))
			return false;
		if (Float.floatToIntBits(frameRate) != Float
				.floatToIntBits(other.frameRate))
			return false;
		if (frameSize != other.frameSize)
			return false;
		if (Float.floatToIntBits(sampleRate) != Float
				.floatToIntBits(other.sampleRate))
			return false;
		if (sampleSizeInBits != other.sampleSizeInBits)
			return false;
		return true;
	}


	/**
	 * Two <code>AudioDataFormat</code>s are equal if all format values
	 * and all properties are equal.
	 * @see #equalFormat(AudioDataFormat)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AudioDataFormat other = (AudioDataFormat) obj;
		if (bigEndian != other.bigEndian)
			return false;
		if (channels != other.channels)
			return false;
		if (encodingClassName == null) {
			if (other.encodingClassName != null)
				return false;
		} else if (!encodingClassName.equals(other.encodingClassName))
			return false;
		if (encodingName == null) {
			if (other.encodingName != null)
				return false;
		} else if (!encodingName.equals(other.encodingName))
			return false;
		if (Float.floatToIntBits(frameRate) != Float
				.floatToIntBits(other.frameRate))
			return false;
		if (frameSize != other.frameSize)
			return false;
		if (properties == null) {
			if (other.properties != null)
				return false;
		} else if (!properties.equals(other.properties))
			return false;
		if (Float.floatToIntBits(sampleRate) != Float
				.floatToIntBits(other.sampleRate))
			return false;
		if (sampleSizeInBits != other.sampleSizeInBits)
			return false;
		return true;
	}


	@Override
	public String toString() {
		return "AudioDataFormat [encodingName=" + encodingName
				+ ", encodingClassName=" + encodingClassName + ", sampleRate="
				+ sampleRate + ", sampleSizeInBits=" + sampleSizeInBits
				+ ", channels=" + channels + ", frameSize=" + frameSize
				+ ", frameRate=" + frameRate + ", bigEndian=" + bigEndian + "]";
	}
	
	
	
}
