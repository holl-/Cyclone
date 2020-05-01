


/**
 * This package provides an abstraction layer
 * for audio playback.
 * 
 * <p>The main interface and entry point is {@link com.mp3player.desktopaudio.AudioEngine} which
 * can be used to obtain information about the audio system as well as create
 * {@link com.mp3player.desktopaudio.Player}s.
 * <code>Player</code> is the main interface for playing back audio.
 * It can pull the audio data either from a {@link com.mp3player.desktopaudio.MediaFile} or a
 * {@link com.mp3player.desktopaudio.MediaStream}.
 * </p>
 * 
 * <p>No audio library (like javax.sound.sampled) is referenced by any of the 
 * classes or interfaces.
 * </p>
 * @author Philipp Holl
 */
package com.mp3player.desktopaudio;