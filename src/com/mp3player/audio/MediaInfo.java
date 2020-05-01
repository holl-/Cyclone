package com.mp3player.audio;

/**
 * MediaInfo provides general information about a media
 * format independent of the media type.
 * @author Philipp Holl
 *
 */
public abstract class MediaInfo {
	protected MediaFormat format;
	
	
	
	public MediaInfo(MediaFormat format) {
		this.format = format;
	}

	
	public MediaFormat getFormat() {
		return format;
	}
	
	/**
	 * Returns the <i>title</i> property of the media or
	 * <code>null</code> if none is contained.
	 * @return the <i>title</i> property or <code>null</code>
	 */
	public abstract String getTitle();
	
	/**
	 * Returns the exact duration of the track in seconds.
	 * If not known, returns <code>-1</code>.
	 * @return the exact duration or <code>-1</code>.
	 * @see #estimateDuration()
	 */
	public abstract double getDuration();
	/**
	 * Calculates an approximation of the media duration.
	 * <p>If the duration is known exactly, this method is equal to
	 * {@link #getDuration()}.
	 * </p>
	 * @return an approximation of the media duration in seconds
	 * @see #getDuration()
	 */
	public abstract double estimateDuration();
	
//	/**
//	 * Calculates an approximation of the number of frames
//	 * in the media.
//	 * @return an approximation of the number of frames
//	 * in the media
//	 */
//	public abstract long estimateFrameLength();
	
	
}
