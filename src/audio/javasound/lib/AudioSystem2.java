package audio.javasound.lib;

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioFormat.Encoding;

import audio.AudioDataFormat;
import audio.AudioEngine;
import audio.MediaFormat;
import audio.MediaType;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

import javazoom.spi.mpeg.sampled.file.MpegEncoding;
import javazoom.spi.mpeg.sampled.file.MpegFileFormatType;
import javazoom.spi.vorbis.sampled.file.VorbisFileFormatType;

public class AudioSystem2 {

	private AudioSystem2() {}
	

	public static AudioFileFormat.Type[] SUPPORTED_TYPES = new AudioFileFormat.Type[] {
		AudioFileFormat.Type.AIFF,
		AudioFileFormat.Type.AU,
		AudioFileFormat.Type.WAVE,
		MpegFileFormatType.MP3,
		VorbisFileFormatType.OGG,
	};
	
	public static AudioFileFormat.Type getFileFormatTypeByExtension(String extension) {
		extension = extension.toLowerCase();
		for(AudioFileFormat.Type type : SUPPORTED_TYPES) {
			if(type.getExtension().equals(extension)) {
				return type;
			}
		}
		return null;
	}
	
	public static AudioFileFormat.Type getFileFormatType(File file) {
		String filename = file.getName();
		return getFileFormatType(filename);
	}
	
	public static AudioFileFormat.Type getFileFormatType(String filename) {
		if(!filename.contains(".")) return null;
		String extension = filename.substring(filename.lastIndexOf('.')+1);
		return AudioSystem2.getFileFormatTypeByExtension(extension);
	}
	
	public static boolean isFormatSupported(File file) {
		return getFileFormatType(file) != null;
	}
	
	public static boolean isFormatSupported(String filename) {
		return getFileFormatType(filename) != null;
	}
	
	
	
	public static AudioFormat convertedFormat(AudioFormat baseFormat) {
	    if(!baseFormat.getEncoding().equals(Encoding.PCM_SIGNED)) {
	    	// Convert to PCM_Signed
		    return new AudioFormat(
		    		Encoding.PCM_SIGNED,
					baseFormat.getSampleRate(),
					16,
					baseFormat.getChannels(),
					baseFormat.getChannels() * 2,
					baseFormat.getSampleRate(),
					false);
	    } else {
	    	return baseFormat;
	    }
	}
	
	private static final MpegEncoding[] MPEG_ENCODINGS = new MpegEncoding[]{
		(MpegEncoding) MpegEncoding.MPEG1L1,
		(MpegEncoding) MpegEncoding.MPEG1L2,
		(MpegEncoding) MpegEncoding.MPEG1L3,
		(MpegEncoding) MpegEncoding.MPEG2L1,
		(MpegEncoding) MpegEncoding.MPEG2L2,
		(MpegEncoding) MpegEncoding.MPEG2L3,
		(MpegEncoding) MpegEncoding.MPEG2DOT5L1,
		(MpegEncoding) MpegEncoding.MPEG2DOT5L2,
		(MpegEncoding) MpegEncoding.MPEG2DOT5L3
	};
	
	public static Encoding getEncoding(String name) {
		for(Encoding e : MPEG_ENCODINGS) {
			if(e.toString().equals(name)) return e;
		}
		return new Encoding(name);
	}
	
	public static AudioInputStream convert(AudioInputStream in) {
		AudioFormat baseFormat = in.getFormat();
	    AudioFormat decodedFormat = convertedFormat(baseFormat);
	    if(baseFormat.equals(decodedFormat))
	    	return in;
	    else
	    	return AudioSystem.getAudioInputStream(decodedFormat, in);
	}
	
	public static AudioInputStream convert(AudioInputStream in, AudioFormat format) {
		if(in.getFormat().equals(format)) return in;
		else {
			return AudioSystem.getAudioInputStream(format, in);
		}
	}
	
	
	public static double[] getMinMaxLineGain(Mixer device) throws LineUnavailableException
	{
		SourceDataLine line = (SourceDataLine) device.getLine(device.getSourceLineInfo()[0]);
		
		line.open();
		FloatControl gain = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);
		double[] minMax = getMinMax(gain);
		line.close();
		return minMax;
	}
	public static double[] getMinMax(FloatControl gain) {
		return new double[]{ gain.getMinimum(), gain.getMaximum() };
	}
	
	
	public static AudioDataFormat toAudioDataFormat(AudioFormat format) {
		String encodingName = format.getEncoding().toString();
		String encodingClassName = format.getEncoding().getClass().getName();
		float sampleRate = format.getSampleRate();
		int sampleSizeInBits = format.getSampleSizeInBits();
		int channels = format.getChannels();
		int frameSize = format.getFrameSize();
		float frameRate = format.getFrameRate();
		boolean bigEndian = format.isBigEndian();
		HashMap<String, Object> properties = serializableMap(format.properties());
		
		return new AudioDataFormat(
				encodingName,
				encodingClassName,
				sampleRate,
				sampleSizeInBits,
				channels,
				frameSize,
				frameRate,
				bigEndian,
				properties);
	}
	
	public static AudioFormat toAudioFormat(AudioDataFormat format) throws UnsupportedAudioFileException {
		Encoding encoding;
		Class<?> encodingClass = null;
		try {
			encodingClass = Class.forName(format.getEncodingClassName());
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		encoding = AudioSystem2.getEncoding(format.getEncodingName());
		if(encoding.getClass() != encodingClass) {
			throw new UnsupportedAudioFileException("encoding instantiated in other class: "+encoding.getClass()+" instead of "+encodingClass);
		}
		return new AudioFormat(
				encoding,
				format.getSampleRate(),
				format.getSampleSizeInBits(),
				format.getChannels(),
				format.getFrameSize(),
				format.getFrameRate(),
				format.isBigEndian(),
				format.getProperties());
	}
	
	public static MediaFormat toMediaFormat(AudioEngine audioEngine, AudioFileFormat format) {
		String formatName = format.getType().toString();
		String formatExtension = format.getType().getExtension();
		int frameLength = format.getFrameLength();
		HashMap<String, Object> properties = serializableMap(format.properties());
		AudioDataFormat audioDataFormat = null;
		if(format.getFormat() != null) {
			audioDataFormat = toAudioDataFormat(format.getFormat());
		}
		MediaType type = new MediaType(formatName, formatExtension);
		return new MediaFormat(audioEngine, type, frameLength, properties, audioDataFormat);
	}
	
	public static AudioFileFormat toAudioFileFormat(MediaFormat format) throws UnsupportedAudioFileException {
		AudioFileFormat.Type type = new AudioFileFormat.Type(format.getType().getName(), format.getType().getFileExtension());
		AudioFormat audioFormat = null;
		if(format.getAudioDataFormat() != null) {
			audioFormat = toAudioFormat(format.getAudioDataFormat());
		}
		return new AudioFileFormat(type, audioFormat, format.getFrameLength(), format.getProperties());
	}
	
	private static HashMap<String, Object> serializableMap(Map<String, Object> properties) {
		HashMap<String, Object> result = new HashMap<String, Object>();
		for(String property : properties.keySet()) {
			Object value = properties.get(property);
			if(value instanceof Serializable) {
				result.put(property, value);
			}
		}
		return result;
	}
}
