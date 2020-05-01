package com.mp3player.audio;

import java.io.IOException;

/**
 * <code>Player</code> is the main interface to play back or obtain information
 * about a <code>MediaFile</code> or <code>MediaStream</code>.
 *
 * <p>One player is bound to one <code>MediaFile</code> or <code>MediaStream</code>
 * which can never change during the player's lifetime.
 * </p>
 *
 * <p>In order to start the player, the following steps are necessary:
 * </p><ul>
 * <li>The player must be prepared using {@link #prepare()}
 * </li>
 * <li>A stream to the sound card must be opened. To open a new channel
 * use {@link #activate(AudioDevice)}.
 * </li>
 * <li>To start playback use {@link #start()}.
 * </li>
 * </ul>
 *
 * @author Philipp Holl
 *
 */
public interface Player {

	/**
	 * Returns the <code>MediaFile</code> associated with this <code>Player</code>.
	 * Each player can only play one media during it's lifetime,
	 * which is defined during the player's creation.
	 * If the player was created from a <code>MediaStream</code>, this method
	 * returns <code>null</code>.
	 * @return the <code>MediaFile</code> associated with this <code>Player</code>
	 * @see #getMediaStream()
	 */
	MediaFile getMediaFile();

	/**
	 * Returns the <code>MediaStream</code> associated with this <code>Player</code>.
	 * Each player can only play one media during it's lifetime,
	 * which is defined during the player's creation.
	 * If the player was created from a <code>MediaFile</code>, this method
	 * returns <code>null</code>.
	 * @return the <code>MediaStream</code> associated with this <code>Player</code>
	 * @see #getMediaFile()
	 */
	MediaStream getMediaStream();

//	/**
//	 *
//	 * @param bufferDuration the length of buffering ahead in seconds,
//	 * <code>-1</code> to buffer all
//	 * @throws UnsupportedOperationException if buffer management is not supported
//	 * @see AudioEngine#isBufferManagementSupported()
//	 */
//	void setBufferAheadDuration(double bufferDuration) throws UnsupportedOperationException;
//	double getBufferAheadDuration();

	/**
	 * Loads the media so it can be played back without delay.
	 *
	 * <p>This method blocks until the media is loaded.
	 * Weather the media is buffered completely or what steps are needed for
	 * playback depends on the implementing class.
	 * The method {@link #estimatePreparationDuration()} gives an approximation
	 * of how long this method will take to complete.
	 * </p>
	 * <p>In the process, the format of the media will also become available,
	 * see {@link #getMediaFormat()}.
	 * </p>
	 * @throws UnsupportedMediaFormatException if the media format is not supported
	 * @throws IOException if the media file cannot be read
	 * @see #isPrepared()
	 * @see #estimatePreparationDuration()
	 * @see #getMediaFormat()
	 */
	void prepare() throws UnsupportedMediaFormatException, IOException;


	/**
	 * A player needs to be prepared in order to play audio or give format information.
	 * Usually {@link #prepare()} must be called before playback can begin,
	 * although a player may not need preparation.
	 * @return true if the player is prepared
	 * @see #prepare()
	 */
	boolean isPrepared();

	/**
	 * Estimates the time it takes to prepare the given media using {@link #prepare()}.
	 * This method may read file format information about
	 * the media object. If the media format is not already loaded, this method call
	 * can take some time to complete, usually under 100ms.
	 * @throws UnsupportedMediaFormatException if the media format is not supported
	 * @throws IOException if the media file cannot be read
	 * @return the approximate time {@link #prepare()} will take in seconds
	 * @see #prepare()
	 */
	double estimatePreparationDuration() throws UnsupportedMediaFormatException, IOException;

	/**
	 * If loaded, this method returns the <code>MediaFormat</code> of this player's media,
	 * otherwise <code>null</code>.
	 * The format might be loaded but is not required to by calling {@link #prepare()}.
	 * To explicitly load the format, use {@link #loadMediaFormat()}.
	 * @return the <code>MediaFormat</code> or <code>null</code> if not loaded
	 * @see #loadMediaFormat()
	 */
	MediaFormat getMediaFormat();
	/**
	 * Both the methods {@link #getMediaFormat()} and {@link #getMediaInfo()} depend on the
	 * media format to be loaded.
	 * Often however, the format will also be loaded by {@link #prepare()}.
	 * This method can be called either before or after {@link #prepare()}.
	 * @throws IOException if the format cannot be read
	 * @throws UnsupportedMediaFormatException if the format is not supported
	 * @throws UnsupportedOperationException if the player is based on a <code>MediaStream</code>
	 * which does not contain format information
	 */
	void loadMediaFormat() throws IOException, UnsupportedMediaFormatException, UnsupportedOperationException;

	/**
	 * Waits until the media duration is known.
	 * If the duration is already known, this method returns immediately.
	 * @throws IllegalStateException if the player is not prepared
	 * @throws InterruptedException if the duration will never be known. This could be because
	 * the player runs into an I/O error.
	 */
	void waitForDurationProperty() throws IllegalStateException, InterruptedException;

	MediaInfo getMediaInfo();

	AudioDataFormat getEncodedFormat();
	AudioDataFormat getDecodedFormat();

	/**
	 * Creates a new <code>MediaStream</code> for this player's media starting at the given position.
	 * The stream will provide audio data as stored in the file without file format information.
	 * If the player is based on a <code>MediaStream</code> providing <i>decoded</i> data, this method will
	 * also return a decoded stream.
	 * <p>Preparing the player is <i>not</i> required for this operation. If the media format is not known, it will
	 * be loaded by this method. The returned <code>MediaStream</code> will therefore know the <code>MediaFormat</code>
	 * returned by {@link MediaStream#getMediaFormat()}.
	 * </p>
	 * @return a new encoded <code>MediaStream</code>
	 * @throws IOException if the media cannot be read
	 * @throws UnsupportedMediaFormatException if the format is not supported
	 * @throws UnsupportedOperationException if streaming is not supported, see {@link AudioEngine#isStreamingSupported()}
	 * or the player is based on a <code>MediaStream</code>.
	 * @see #newDecodedStream(double)
	 */
	MediaStream newEncodedStream() throws IOException, UnsupportedMediaFormatException, UnsupportedOperationException;

	/**
	 *
	 * @param startPosition the start position in seconds
	 * @return a new decoded <code>MediaStream</code> starting at the given position
	 * @throws IOException if the media cannot be read
	 * @throws UnsupportedMediaFormatException if the format is not supported
	 * @throws IllegalStateException if the player is not prepared
	 * @throws UnsupportedOperationException if streaming is not supported
	 */
	MediaStream newDecodedStream(double startPosition) throws IOException, UnsupportedMediaFormatException, IllegalStateException, UnsupportedOperationException;


	/**
	 * If a player is active, it has a line to itself and can start playing audio.
	 *
	 * <p>A player deactivates itself after finishing playback if and only if another player was queued
	 * using {@link #setNext(Player)}.
	 * The deactivation can even happen before audio playback stops, because an internal buffer
	 * might still contain audio data of this player after it is deactivated.
	 * </p>
	 * <p>As many systems only have a limited amount of audio lines, players can be
	 * deactivated to free the resource for other players while keeping
	 * the media status and buffer.
	 * An approximate maximum of simulatenously active players can be obtained
	 * using {@link AudioDevice#getMaxActivePlayers()}.
	 * </p>
	 * @return the active status
	 * @see #activate(AudioDevice)
	 * @see #deactivate()
	 * @see AudioDevice#getMaxActivePlayers()
	 */
	boolean isActive();
	/**
	 *
	 * @param device the device to play on
	 * @throws AudioEngineException if activating the media fails
	 * @throws IllegalStateException if the player is not prepared
	 */
	void activate(AudioDevice device) throws AudioEngineException, IllegalStateException;
	void deactivate();

	void addActivationListener(PlayerEventListener l);
	void removeActivationListener(PlayerEventListener l);


	AudioDevice getDevice();
	/**
	 *
	 * This method blocks until playback on the new device has begun.
	 * Calling this method with the current device will do nothing.
	 * @param device the new device to play on
	 * @throws AudioEngineException if opening the given device causes an error
	 * @throws IllegalStateException if the player is not active
	 */
	void switchDevice(AudioDevice device) throws AudioEngineException, IllegalStateException;

	/**
	 * Tests if the audio data of the given player can be played through
	 * this player's channel.
	 * If true, {@link #setNext(Player)} can be called with the other player.
	 * @param next a different player
	 * @throws IllegalStateException if the other player's format is not known or it is not prepared
	 * @return true if {@link #setNext(Player)} can be called with the given player
	 * @see #setNext(Player)
	 */
	boolean canSetNext(Player next) throws IllegalStateException;
	/**
	 * Sets a player to play directly after this one finishes, through this player's
	 * channel. The two audio data streams will be merged, so no delay between the two
	 * players can appear. The other player will start at it's current position.
	 * <p>This can only be used under specific circumstances. Use {@link #canSetNext(Player)}
	 * to see if the method is supported with a given player.
	 * </p>
	 * If the given player is not prepared, an IllegalStateException is thrown.
	 * If for other reasons, {@link #canSetNext(Player)} would return false, an
	 * IllegalArgumentException is thrown.
	 * <p>The other player needs to be deactivated when this player finishes.
	 * If it isn't, the behavious is unspecified.</p>
	 * <p>Pass <code>this</code> player as <code>next</code> to create an indefinite
	 * loop. The above limitation does not apply to this case.
	 * </p>
	 * @param next the player to start after this one finishes
	 * @throws IllegalStateException if the other player is not prepared
	 * @throws IllegalArgumentException if the other player cannot be set as next
	 * @see #canSetNext(Player)
	 * @see #getNext()
	 */
	void setNext(Player next) throws IllegalStateException, IllegalArgumentException;

	Player getNext();

//	/**
//	 * Queues the given player to start when <code>this</code> player has finished.
//	 * In contrast to {@link #setNext(Player)}, this method works with all players,
//	 * even ones of a different {@link AudioEngine}.
//	 * <p>This method blocks until the next player has started playing.
//	 * </p>
//	 * <p>The given player need not be prepared. If it isn't already, it will be
//	 * prepared in time before this player finishes.
//	 * </p>
//	 * <p>If the argument is <code>null</code>, nothing will happen, when this player
//	 * finishes.
//	 * </p>
//	 * @param next the player to start after this player
//	 * @throws InterruptedException when {@link #queueBlocking(Player)} or
//	 * {@link #setNext(Player)} are called on a different thread while this thread
//	 * is blocking.
//	 * @throws AudioEngineException
//	 * @throws IOException
//	 * @throws UnsupportedMediaFormatException
//	 */
//	void queueBlocking(Player next) throws InterruptedException, AudioEngineException, IOException, UnsupportedMediaFormatException;
	// TODO move to util class


	/**
	 * Returns a reference to the data buffer of this player.
	 * If direct access to the buffer is not supported by the implementing
	 * class, this method returns <code>null</code>.
	 * <p>If supported, the buffer will be created during {@link #prepare()}.
	 * Calling the method beforehand, will return <code>null</code>.
	 * </p>
	 * @return a reference to the data buffer
	 */
	AudioBuffer getAudioBuffer();


	/**
	 * Releases all resources associated with this player and returns when
	 * the disposal is completed.
	 * This method will also {@link #deactivate()} the player if it is
	 * still active.
	 */
	void dispose();



    /**
     * Starts playing this player's media.
     * If the player is not active ({@link #isActive()}), this method
     * throws an IllegalStateException
     * @throws IllegalStateException if the player is not active
     */
	void start() throws IllegalStateException;
	void pause();
	boolean isPlaying();

	void addStateListener(PlayerEventListener l); // playing
	void removeStateListener(PlayerEventListener l);



	/**
	 * Returns the current position in seconds.
	 * If the player is not prepared, this method returns <code>-1</code>.
	 * @return the current position in seconds
	 * @throws IllegalStateException if no media is loaded
	 */
	double getPosition() throws IllegalStateException;
	/**
	 * Seeks the given position in the track, blocking until the current position has been updated.
	 * @param position the target position in seconds
	 * @param timeout timeout in seconds
	 * @throws InterruptedException if seeking is interrupted or in case of timeout
	 * @throws IOException
	 */
	void setPositionBlocking(double position, double timeout) throws InterruptedException, IOException;
	void setPositionAsync(double position);
	/**
	 * Returns the media duration in seconds.
	 * <p>If the duration is not known, this method returns <code>-1</code>.
	 * In this case, if buffer access is supported, {@link AudioBuffer#getEndPosition()}
	 * can be used to get the currently known minimum duration.
	 * </p>
	 * @return the media duration in seconds
	 */
	double getDuration();


	void addEndOfMediaListener(PlayerEventListener l);
	void removeEndOfMediaListener(PlayerEventListener l);


	void addMarker(double position, MarkerListener l);
	void removeMarker(MarkerListener l);


	double getGain();
	void setGain(double dB) throws IllegalStateException;

	boolean isMute();
	void setMute(boolean mute) throws IllegalStateException;

	/**
	 * Retrieves the left-right volume. <code>-1</code> is only left speaker while
	 * <code>1</code> is only right speaker.
	 * @return the audio balance
	 */
	double getBalance();
	void setBalance(double balance) throws IllegalStateException;

}
