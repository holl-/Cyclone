package audio

import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.value.*
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.scene.media.MediaPlayer
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.Serializable
import java.util.logging.Logger
import javax.sound.sampled.AudioFormat

/**
 * Library interface for handling audio files and playing back audio.
 * Only one instance of `AudioEngine` is needed by an application to use the library.
 */
abstract class AudioEngine(val name: String)
{
    abstract val supportedMediaTypes: ObservableList<MediaType>
    /** All devices that can be targeted by this [AudioEngine] */
    abstract val devices: ObservableList<AudioDevice>
    /** All living players. */
    abstract val players: ObservableList<Player>
    /** All living buffers. There must be at least one buffer for each mediaId that is currently playing. */
    abstract val buffers: ObservableList<AudioBuffer>
    /** Currently living device streams. */
    abstract val lines: ObservableList<AudioLine>

	var logger: Logger? = null

    /** Creates a new player for the given media. If a player for this media already exists, they might share a single buffer, depending on the implementation. */
    @Throws(IOException::class, UnsupportedMediaFormatException::class) abstract fun createPlayer(file: File, mediaId: Any): Player
    @Throws(IOException::class, UnsupportedMediaFormatException::class) abstract fun createPlayer(stream: InputStream, size: Long, mediaId: Any): Player

    /** Frees all resources associated with the [AudioEngine]. */
    abstract fun dispose()

    /** Preferred playback buffer time in seconds. This refers to the buffer that is used when writing audio data to the device, not the media buffer. */
    val outputBufferTime = SimpleDoubleProperty(0.2)
}


abstract class MediaFile(val mediaId: Any, val fileSize: Long, val engine: AudioEngine) {
    abstract val backingFile: ObservableValue<File?>
    /** How much of the media file has been read. If there is a local copy of the file, this should be equal to [fileSize] */
    abstract val availableFileSize: ObservableLongValue
    /** Whether this buffer currently has an in-memory representation. If not, [startPosition] and [endPosition] must both be 0.0 */
}


/**
 * In order to start the player, the following steps are necessary:
 *  * Player creation using [AudioEngine.createPlayer]
 *  * Device selection using [setOutput]
 *  * Start playing by setting [paused] = false
 */
abstract class Player(val file: MediaFile) {
    val gain = SimpleDoubleProperty()
    val mute = SimpleBooleanProperty()
    /** Left (-1), center (0), Right (1) */
    val balance = SimpleDoubleProperty()
    val paused = SimpleBooleanProperty(true)
    /** Default is media duration */
    val stopTime = SimpleObjectProperty<Double?>()

    abstract val status: ObservableValue<MediaPlayer.Status>  // Unknown, Ready, Paused, Playing, Stalled, Stopped, Halted (error), Disposed
    abstract val duration: ObservableValue<Double?>
    abstract val position: ObservableDoubleValue
    /** The output stream if the player is active, if available. */
    abstract val audioLine: ObservableValue<AudioLine?>
    abstract val error: ObservableValue<java.lang.Exception?>
    abstract val encodedFormat: ObservableValue<AudioFormat?>
    abstract val buffer: AudioBuffer


    /** Opens a stream to the device and closes the previous stream. If [device]=null, closes the stream. This cannot be set while the player is not ready. */
    abstract fun setOutput(device: AudioDevice?)

    /** Seeks the given position in the media, returning immediately. */
    abstract fun seek(position: Double)

    /** Frees all resources associated with this player. */
    abstract fun dispose()

    val engine: AudioEngine
        get() = file.engine
}


abstract class AudioBuffer(val file: MediaFile) {
    abstract val format: ObservableValue<AudioFormat>
    abstract val inMemory: ObservableBooleanValue
    /** Whether this buffer is currently being written to */
    abstract val growing: ObservableBooleanValue
    /** Memory allocated by this buffer in bytes. If the size is not known`, this method returns either an estimate or null. */
    abstract val allocatedMemory: ObservableObjectValue<Long?>
    /** Start position of the buffer within the file in seconds. */
    abstract val startPosition: ObservableDoubleValue
    /** End position of the buffer within the file in seconds. */
    abstract val endPosition: ObservableDoubleValue

    val engine: AudioEngine
        get() = file.engine
}


abstract class AudioLine(val device: AudioDevice, val format: AudioFormat, val bufferSizeInBytes: Int) {
    abstract val alive: ObservableBooleanValue
    abstract val playing: ObservableBooleanValue
    val paused = SimpleBooleanProperty()  // can be set by user
    val playerQueue = FXCollections.observableArrayList<Player>()  // can be edited by user
}


/**
 * @param fileExtension the file extension in lower case, e.g. "mp3", "wav".
 */
data class MediaType(val name: String, val fileExtension: String) : Serializable


/**
 * @param maxActivePlayers approximate maximum of players that can be active
 * simultaneously on this device.
 * Null if no such limit exists.
 */
data class AudioDevice(
        val name: String,
        val id: String,
        val isDefault: Boolean,
        val maxGain: Double,
        var minGain: Double,
        val maxActivePlayers: Int?
)


data class MediaStream(
        val mediaId: Any,
        val stream: InputStream,
        val streamLength: Long,
        val frameLength: Long,
        val startFrame: Long,
        val format: AudioFormat,
        val mediaType: MediaType) : Serializable


class AudioEngineException(message: String?, cause: Throwable?) : Exception(message, cause)

class UnsupportedMediaFormatException(msg: String?, cause: Throwable?) : Exception(msg, cause)
