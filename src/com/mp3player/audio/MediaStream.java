package com.mp3player.audio;

import java.io.InputStream;

/**
 * The <code>MediaStream</code> interface represents a stream of audio
 * bytes of which the format is known.
 * 
 * <p>Example situations where this can be used include:</p>
 * <ul><li>Streaming a file over network, beginning somewhere in the middle</li>
 * <li>Playing generated sound</li>
 * </ul>
 * 
 * 
 * @author Philipp Holl
 *
 */
public class MediaStream {
	private InputStream stream;
	private long streamLength;
	private long frameLength;
	private long startFrame;
	private AudioDataFormat audioDataFormat;
	private MediaFormat mediaFormat;
	
	
	public MediaStream(InputStream stream, long streamLength, long frameLength,
			long startFrame, AudioDataFormat audioDataFormat,
			MediaFormat mediaFormat) {
		this.stream = stream;
		this.streamLength = streamLength;
		this.frameLength = frameLength;
		this.startFrame = startFrame;
		this.audioDataFormat = audioDataFormat;
		this.mediaFormat = mediaFormat;
	}

	/**
	 * Returns the audio data stream.
	 * The stream only contains audio data (no format information)
	 * and is of the format returned by {@link #getAudioDataFormat()}.
	 * @return the audio data stream
	 * @see #getAudioDataFormat()
	 */
	public InputStream getStream() {
		return stream;
	}
	
	/**
	 * Returns the stream length in bytes or <code>-1</code>
	 * if unknown.
	 * @return the stream length in bytes or <code>-1</code>
	 * if unknown
	 */
	public long getStreamLength() {
		return streamLength;
	}
	
	/**
	 * Returns the number of frames in this stream or <code>-1</code>
	 * if unknown.
	 * @return the number of frames in this stream or <code>-1</code>
	 * if unknown
	 */
	public long getFrameLength() {
		return frameLength;
	}
	
	/**
	 * Returns the number of the first frame provided by the stream.
	 * If the frame number is unknown or no such number exists, this 
	 * method return <code>-1</code>.
	 * @return the number of the first frame provided by the stream
	 */
	public long getStartFrame() {
		return startFrame;
	}
	
	/**
	 * Returns the <code>AudioDataFormat</code> of the stream.
	 * This can either be an encoded or decoded stream.
	 * 
	 * <p>If {@link #getMediaFormat()} is also supported, the 
	 * <code>AudioDataFormat</code> contained in the <code>MediaFormat</code>
	 * may not be equal to the one returned by this method, as
	 * {@link MediaFormat#getAudioDataFormat()} usually returns the
	 * encoded audio format.
	 * </p>
	 * @return the <code>AudioDataFormat</code> of the stream
	 * @see #getMediaFormat()
	 * @see #getStream()
	 */
	public AudioDataFormat getAudioDataFormat() {
		return audioDataFormat;
	}
	
	/**
	 * Returns the <code>MediaFormat</code> of this stream
	 * or <code>null</code> if not available.
	 * Note that the <code>AudioDataFormat</code> contained in the
	 * <code>MediaFormat</code> may not match this stream, as it usually
	 * represents the encoded format.
	 * @return the <code>MediaFormat</code> of this stream
	 * or <code>null</code> if not available
	 * @see #getAudioDataFormat()
	 */
	public MediaFormat getMediaFormat() {
		return mediaFormat;
	}
	
}
