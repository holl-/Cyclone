package com.mp3player.audio.javasound.lib;

import java.util.Map;

import com.mp3player.audio.MediaFormat;
import com.mp3player.audio.MediaInfo;



/**
 * http://www.javazoom.net/mp3spi/documents.html
 * @author Philipp
 *
 */
public class MP3Info extends MediaInfo
{
	// Tag Properties
	private static final String TITLE = "title";
	private static final String AUTHOR = "author";
	private static final String ALBUM = "album";
	private static final String DATE = "date";
	private static final String COMMENT = "comment";
	private static final String TRACK = "mp3.id3tag.track";
	private static final String GENRE = "mp3.id3tag.genre";
	private static final String COPYRIGHT = "copyright";

	// Frame Properties
	private static final String HAS_COPYRIGHT = "mp3.copyright";
	private static final String ORIGINAL = "mp3.original";
	private static final String MODE = "mp3.mode";

	// Generated Properties
	/** microsecond duration */
	private static final String DURATION = "duration";
	private static final String MP3_LENGTH_FRAMES = "mp3.length.frames";
	
	
	
	enum Mode
	{
		STEREO, JOINT_STEREO, DUAL_CHANNEL, SINGLE_CHANNEL
	}
	
	
	

	public MP3Info(MediaFormat format) {
		super(format);
	}


	

	public void printProperties() {
		System.out.println("File Properties");
		printProperties(format.getProperties());
		System.out.println("MP3 Properties");
		printProperties(format.getAudioDataFormat().getProperties());
		
	}
	
	private void printProperties(Map<String, Object> props) {
		for(String key : props.keySet()) {
			Object value = props.get(key);
			if(value == null) {
				System.out.println("\t"+key+": "+value);
			} else {
				Class<?> clazz = value.getClass();
				System.out.println("\t"+key+": "+value+" ["+clazz.getSimpleName()+"]");
			}
		}
	}
	
	
	private Object fileprop(String key) {
		return format.getProperty(key);
	}

	@Override
	public String getTitle() {
		return (String) fileprop(TITLE);
	}

	@Override
	public double getDuration() {
		long propDuration = (Long) fileprop(DURATION);
		if(propDuration > 0) return propDuration / 1000_000.0;
		else return -1;
	}
		
	public double estimateDuration() {
		double exact = getDuration();
		if(exact != -1) return exact;
		
		long frameLength = format.getFrameLength();
		if(frameLength < 0) frameLength = estimateFrameLength();
		double durationSec = frameLength / format.getAudioDataFormat().getFrameRate();
		return durationSec;
	}
	
	/**
	 * Returns the mp3 property which should be equal to the calculated result.
	 */
	public long estimateFrameLength() {
		int propertyFrameLength = (Integer) fileprop(MP3_LENGTH_FRAMES);
		if(propertyFrameLength > 0) return propertyFrameLength;
		
		// Estimate frame length
		int frameSizeBytes = (Integer) fileprop("mp3.framesize.bytes");
		int bytes = (Integer) fileprop("mp3.length.bytes");
		int frames = bytes / frameSizeBytes;
		return frames;
	}
	
	
	
	public String getAuthor() {
		return (String) fileprop(AUTHOR);
	}
	
	public String getAlbum() {
		return (String) fileprop(ALBUM);
	}
	
	public String getDate() {
		return (String) fileprop(DATE);
	}
	
	public String getCopyright() {
		return (String) fileprop(COPYRIGHT);
	}
	
	public boolean hasCopyright() {
		return (boolean) fileprop(HAS_COPYRIGHT);
	}
	
	public String getComment() {
		return (String) fileprop(COMMENT);
	}
	
	public String getTrack() {
		return (String) fileprop(TRACK);
	}
	
	public int getTrackNumber() {
		String val = (String) fileprop(TRACK);
		if(val == null) return -1;
		try {
			return Integer.parseInt(val);
		} catch(NumberFormatException exc) {
			return -1;
		}
	}
	
	public String getGenre() {
		return (String) fileprop(GENRE);
	}
	
	public Mode getMode() {
		int mode = (int) fileprop(MODE);
		switch(mode) {
		case 0: return Mode.STEREO;
		case 1: return Mode.JOINT_STEREO;
		case 2: return Mode.DUAL_CHANNEL;
		case 3: return Mode.SINGLE_CHANNEL;
		default: return null;
		}
	}
	
	public boolean isOriginal() {
		return (boolean) fileprop(ORIGINAL);
	}
	
	
}
