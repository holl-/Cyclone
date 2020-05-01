package com.mp3player.audio;

import java.util.Collection;
import java.util.List;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.mp3player.audio.util.SystemTimeManager;

/**
 * <code>AudioEngine</code> represents a backend interface for playing audio.
 * Only one instance of <code>AudioEngine</code> is needed by an application to
 * use the library.
 *
 * @author Philipp Holl
 *
 */
public abstract class AudioEngine {
	private String name;
	protected Logger logger;

	/**
	 * Creates a new AudioEngine and initializes it.
	 */
	public AudioEngine(String name) {
		this.name = name;

		logger = Logger.getLogger(name);
		logger.setLevel(Level.ALL);
		ConsoleHandler cs = new ConsoleHandler();
		cs.setLevel(Level.ALL);
		logger.addHandler(cs);
	}

	/**
	 * Creates a new player for the given media. The player is not prepared and
	 * no data will be read during the creation process.
	 * <p>
	 * If a player for this media already exists, they might share a single
	 * buffer, depending on the implementation.
	 * </p>
	 *
	 * @param media
	 *            the player's <code>MediaFile</code> object
	 * @return a new player for the given media
	 */
	public abstract Player newPlayer(MediaFile media);

	/**
	 *
	 * @param stream
	 *            the stream to use as a source for the player
	 * @return a new player for the given source stream
	 * @throws UnsupportedOperationException
	 *             if streaming is not supported, see
	 *             {@link #isStreamingSupported()}
	 */
	public abstract Player newPlayer(MediaStream stream) throws UnsupportedOperationException;

	/**
	 * Returns a list of all players that were created using one of the
	 * <code>newPlayer</code> methods and have not been disposed of using
	 * {@link Player#dispose()}.
	 *
	 * @return list of existing players
	 */
	public abstract List<Player> getPlayers();

	public abstract Collection<MediaType> getSupportedMediaTypes();

	public boolean isFormatSupported(MediaFile media) {
		return getMediaType(media) != null;
	}

	public MediaType getMediaType(MediaFile media) {
		String filename = media.getFileName();
		if (filename == null)
			throw new IllegalArgumentException("media = null");
		if (!filename.contains("."))
			return null;
		String extension = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();

		for (MediaType type : getSupportedMediaTypes()) {
			if (type.getFileExtension().equals(extension))
				return type;
		}
		return null;
	}

	public abstract AudioDevice getDefaultDevice();

	public abstract List<AudioDevice> getDevices();

	public Logger getLogger() {
		return logger;
	}

	public void setLogger(Logger log) {
		logger = log;
	}

	/**
	 * Releases all resources associated with the <code>AudioEngine</code>. If
	 * there are sill active players, they will also be destroyed.
	 */
	public abstract void dispose();

	// Properties
	/**
	 * Returns the unique name of the <code>AudioEngine</code> implementation.
	 * This could be <code>"Java Sound"</code>. The String can be used as an
	 * identifier of the implementation.
	 *
	 * @return the unique name of the <code>AudioEngine</code> implementation
	 */
	public final String getName() {
		return name;
	}

	/**
	 * Tests if the player can play audio from a {@link MediaStream}.
	 *
	 * @return true if the player can play audio from a {@link MediaStream}
	 */
	public abstract boolean isStreamingSupported();

	public abstract boolean isBufferManagementSupported();

	// private Thread pauseOnStandbyThread; TODO pause on standby events with
	// PlayerEvent.EXTERNAL_INTERRUPT
	private SystemTimeManager stm;

	public void setPauseOnStandby(boolean pauseOnStandby) {
		if (pauseOnStandby && stm == null) {
			stm = new SystemTimeManager();
			stm.addSystemTimeJumpListener(new SystemTimeManager.SystemTimeJumpListener() {
				@Override
				public void timeJumped(long timeDifference) {
					getLogger().fine("System time jumped by " + timeDifference + " ms. Pausing all Players");
					for (Player player : getPlayers()) {
						player.pause();
					}
				}
			});
			stm.start(50);
		} else if (!pauseOnStandby && stm != null) {
			stm.dispose();
			stm = null;
		}
	}

	public boolean getPauseOnStandby() {
		return stm != null;
	}
}
