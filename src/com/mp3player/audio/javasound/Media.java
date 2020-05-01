package com.mp3player.audio.javasound;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import com.mp3player.audio.javasound.lib.AudioSystem2;
import com.mp3player.audio.javasound.lib.MemoryAudioBuffer;
import com.mp3player.audio.AudioDataFormat;
import com.mp3player.audio.MediaFile;
import com.mp3player.audio.MediaFormat;
import com.mp3player.audio.MediaInfo;
import com.mp3player.audio.MediaStream;
import com.mp3player.audio.UnsupportedMediaFormatException;

public class Media
{
	private JavaSoundEngine engine;

	// Source
	private MediaFile mediaFile;
	private MediaStream stream;

	// Objects after Preparation
	private MediaInfo info;
	private AudioDataFormat encodedAudioFormat;
	private AudioDataFormat decodedAudioFormat;
	private MemoryAudioBuffer buffer;
	private CountDownLatch bufferFilledLatch;

	private List<Object> users = new CopyOnWriteArrayList<Object>();


	public Media(JavaSoundEngine engine, MediaFile mediaFile) {
		this.engine = engine;
		this.mediaFile = mediaFile;
	}


	public Media(JavaSoundEngine engine, MediaStream stream) {
		this.engine = engine;
		this.stream = stream;
	}



	public void prepare() throws UnsupportedMediaFormatException, IOException
	{
		if(isPrepared() && buffer.exists()) return;

		if(encodedAudioFormat == null) {
			// Load MediaFile information
			try {
				loadMediaFormat();
			} catch (UnsupportedOperationException e1) {
				// MediaStream format not known, info = null
			}
		}


		// Create AudioInputStream from either MediaFile or MediaStream
		AudioInputStream in;
		if(mediaFile != null) { // MediaFile
			in = openMedia(mediaFile);
		} else { // MediaStream
			AudioFormat streamAf;
			try {
				streamAf = AudioSystem2.toAudioFormat(stream.getAudioDataFormat());
			} catch (UnsupportedAudioFileException e) {
				throw new UnsupportedMediaFormatException(e);
			}
			in = new AudioInputStream(stream.getStream(), streamAf, stream.getFrameLength());
		}
		engine.getLogger().finer("Opened mediaFile stream "+in.getFormat());


		// Decode stream
	    AudioInputStream decodedStream = AudioSystem2.convert(in);
	    engine.getLogger().finer("Decoded format is "+decodedStream.getFormat());

	    decodedAudioFormat = AudioSystem2.toAudioDataFormat(decodedStream.getFormat());


	    // Create and fill buffer
	    long frameLength = decodedStream.getFrameLength();
	    buffer = new MemoryAudioBuffer(decodedStream.getFormat(), frameLength);
	    bufferFilledLatch = new CountDownLatch(1);

		buffer.startFilling(decodedStream,
	    		() -> {
	    			bufferFilledLatch.countDown();
	    			engine.getLogger().fine("Buffer Filled "+buffer);
    			},
	    		() -> {
	    			bufferFilledLatch.countDown();
	    			engine.getLogger().warning("Buffer closed before filled "+buffer);
    			});

	}

	public void waitUntilBufferFilled() throws InterruptedException {
		bufferFilledLatch.await();
	}


	private static AudioInputStream openMedia(MediaFile media) throws UnsupportedMediaFormatException, IOException {
		if(media.getFile() != null) {
			try {
				return AudioSystem.getAudioInputStream(media.getFile());
			} catch (UnsupportedAudioFileException e) {
				throw new UnsupportedMediaFormatException(e);
			}
		}
		else {
			InputStream mediaIn = media.openStream();
			if(!mediaIn.markSupported()) {
				mediaIn = new BufferedInputStream(mediaIn);
			}
			try {
				return AudioSystem.getAudioInputStream(mediaIn);
			} catch (UnsupportedAudioFileException e) {
				throw new UnsupportedMediaFormatException(e);
			}
		}
	}


	public MediaStream newEncodedStream() throws IOException,
			UnsupportedMediaFormatException {
		if(mediaFile != null) {
			if(info == null) loadMediaFormat();

			AudioInputStream in = openMedia(mediaFile);
			AudioFormat af = in.getFormat();
			long frameLength = in.getFrameLength();
			long streamLength = (frameLength < 0 || af.getFrameSize() < 0) ? -1 : frameLength * af.getFrameSize();

			return new MediaStream(in,
					streamLength, frameLength, 0,
					encodedAudioFormat, info.getFormat());
		}
		else { // MediaStream
			throw new UnsupportedOperationException("not supported by a MediaStream player");
		}
	}

	public MediaStream newDecodedStream(int startPositionMillis) throws IOException,
			UnsupportedMediaFormatException {
		if(!isPrepared()) throw new IllegalStateException("player not prepared");
		int frame = buffer.getFrame(startPositionMillis);
		int frameLength = buffer.getFrameLength() < 0 ? -1 : buffer.getFrameLength() - frame;
		int streamLength = frameLength * buffer.getFormat().getFrameSize();

		return new MediaStream(
				buffer.audioStreamFromFrame(frame),
				streamLength,
				frameLength,
				frame,
				decodedAudioFormat,
				getMediaFormat());
	}

	public boolean isPrepared() {
		return buffer != null;
	}

	public double estimatePreparationDuration()
			throws UnsupportedMediaFormatException, IOException {
		return 0.15;
	}

	/**
	 * Always loads encodedAudioFormat
	 * and info if supported
	 */
	public void loadMediaFormat() throws IOException,
			UnsupportedMediaFormatException, UnsupportedOperationException
	{
		if(mediaFile != null) {
			AudioFileFormat aff;

			if(mediaFile.getFile() == null) {
				// Obtain format from InputStream
				InputStream mediaIn = mediaFile.openStream();
				if(!mediaIn.markSupported()) {
					mediaIn = new BufferedInputStream(mediaIn);
				}
				try {
					aff = AudioSystem.getAudioFileFormat(mediaIn);
				} catch (UnsupportedAudioFileException e) {
					throw new UnsupportedMediaFormatException(e);
				}
				mediaIn.close();
			} else {
				// Use the file instead
				try {
					aff = AudioSystem.getAudioFileFormat(mediaFile.getFile());
				} catch (UnsupportedAudioFileException e) {
					throw new UnsupportedMediaFormatException(e);
				}
			}
			MediaFormat format = AudioSystem2.toMediaFormat(engine, aff);
			info = engine.createMediaInfo(mediaFile, format);
			encodedAudioFormat = format.getAudioDataFormat();
		}
		else // MediaStream
		{
			MediaFormat format = stream.getMediaFormat();
			if(format != null) {
				info = engine.createMediaInfo(stream);
				encodedAudioFormat = format.getAudioDataFormat();
			} else {
				encodedAudioFormat = stream.getAudioDataFormat();
				throw new UnsupportedOperationException("format of stream unknown");
			}
		}

	}

	public MediaFormat getMediaFormat() {
		if(info == null) return null;
		return info.getFormat();
	}


	public JavaSoundEngine getEngine() {
		return engine;
	}


	public MediaFile getMediaFile() {
		return mediaFile;
	}


	public MediaStream getStream() {
		return stream;
	}


	public MediaInfo getInfo() {
		return info;
	}


	public AudioDataFormat getEncodedAudioFormat() {
		return encodedAudioFormat;
	}


	public AudioDataFormat getDecodedAudioFormat() {
		return decodedAudioFormat;
	}


	public MemoryAudioBuffer getBuffer() {
		return buffer;
	}


	public void addUser(Object user) {
		users.add(user);
	}

	public void removeUserDealloc(Object user) {
		users.remove(user);
		if(users.isEmpty()) {
			dealloc();
		}
	}


	public void dealloc() {
		buffer.dealloc(true);
	}


	public double getDuration() {
		if(buffer.isClosed()) return buffer.getDurationMicros() / 1000_000.0;
		else return info.getDuration();
	}

}
