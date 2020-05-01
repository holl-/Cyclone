


/**
 * This package provides an abstraction layer
 * for audio playback.
 * 
 * <p>The main interface and entry point is {@link audio.AudioEngine} which
 * can be used to obtain information about the audio system as well as create
 * {@link audio.Player}s.
 * <code>Player</code> is the main interface for playing back audio.
 * It can pull the audio data either from a {@link audio.MediaFile} or a
 * {@link audio.MediaStream}.
 * </p>
 * 
 * <p>No audio library (like javax.sound.sampled) is referenced by any of the 
 * classes or interfaces.
 * </p>
 * @author Philipp Holl
 */
package audio;