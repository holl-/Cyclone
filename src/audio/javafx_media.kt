package audio

import javafx.beans.binding.Bindings
import javafx.beans.property.SimpleLongProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.value.*
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.scene.media.Media
import javafx.scene.media.MediaException
import javafx.scene.media.MediaPlayer
import javafx.util.Duration
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.Callable
import javax.sound.sampled.AudioFormat


class JavaFXAudioEngine : AudioEngine("JavaFX")
{
    override val supportedMediaTypes = FXCollections.observableArrayList(
            MediaType("MP3", "mp3"),
            MediaType("AIFF", "aif"),
            MediaType("WAVE", "wav")
    )
    override val devices = FXCollections.observableArrayList(AudioDevice("Primary device", "FX", true, 0.0, Double.NEGATIVE_INFINITY, null))

    override val players: ObservableList<Player> = FXCollections.observableArrayList()
    override val buffers: ObservableList<AudioBuffer> = FXCollections.observableArrayList()
    override val lines: ObservableList<AudioLine> = FXCollections.emptyObservableList()

    private val mediaMap = HashMap<Any, FXMediaFile>()

    override fun createPlayer(file: File, mediaId: Any): Player {
        synchronized(this) {
            val media = mediaMap[mediaId] ?: run {
                val m = FXMediaFile(mediaId, file.length(), this)
                m.initFromFile(file)
                mediaMap[mediaId] = m
                m
            }
            val player = FXPlayer(media)
            players.add(player)
            buffers.add(player.buffer)
            return player
        }
    }

    override fun createPlayer(stream: InputStream, size: Long, mediaId: Any): Player {
        synchronized(this) {
            val media = mediaMap[mediaId] ?: run {
                val m = FXMediaFile(mediaId, size, this)
                m.initFromStream(stream)
                mediaMap[mediaId] = m
                m
            }
            val player = FXPlayer(media)
            players.add(player)
            buffers.add(player.buffer)
            return player
        }
    }

    override fun dispose() {
        logger?.fine("Disposing of JavaFXAudioEngine")
        for (player in players) {
            player.dispose()
        }
    }
}


class FXMediaFile(mediaId: Any, fileSize: Long, engine: AudioEngine) : MediaFile(mediaId, fileSize, engine) {
    internal val media = SimpleObjectProperty<Media?>()
    internal val error = SimpleObjectProperty<Exception>()

    override val backingFile: ObservableValue<File?> = SimpleObjectProperty()
    override val availableFileSize: ObservableLongValue = SimpleLongProperty()


    fun initFromFile(file: File) {
        val uri = file.toURI().toString()
        try {
            media.value = Media(uri)
        } catch (exc: MediaException) {
            exc.printStackTrace()
            if (exc.type == MediaException.Type.MEDIA_UNAVAILABLE || exc.type == MediaException.Type.MEDIA_INACCESSIBLE) error.value = IOException(exc)
            error.value = UnsupportedMediaFormatException(null, exc)
        } catch (exc: UnsupportedOperationException) {
            error.value =  IOException(exc)
        }
    }

    fun initFromStream(stream: InputStream) {
        try {
            val file = File.createTempFile("stream", mediaId.toString())
            FileOutputStream(file).use { out -> stream.use { it.transferTo(out) } }
            val uri = file.toURI().toString()
            media.value = Media(uri)
        } catch (exc: MediaException) {
            if (exc.type == MediaException.Type.MEDIA_UNAVAILABLE || exc.type == MediaException.Type.MEDIA_INACCESSIBLE) error.value =  IOException(exc)
            error.value =  UnsupportedMediaFormatException(null, exc)
        } catch (exc: UnsupportedOperationException) {
            error.value =  IOException(exc)
        } catch (exc: IOException) {
            error.value = exc
        }
    }
}


class FXPlayer(file: FXMediaFile) : Player(file)
{
    private val player = SimpleObjectProperty<MediaPlayer>()
    override val buffer: AudioBuffer = FXBuffer(player, file)

    override val status: ObservableValue<MediaPlayer.Status>
        get() = player.statusProperty()
    override val duration: ObservableValue<Double?>
        get() = player.media.durationProperty().seconds()
    override val position: ObservableDoubleValue
        get() = Bindings.createDoubleBinding(Callable { player.currentTime.toSeconds() }, player.currentTimeProperty())
    override val audioLine = SimpleObjectProperty<AudioLine>(null)
    override val error: ObservableValue<Exception?>
        get() = player.errorProperty() as ObservableValue<Exception?>
    override val encodedFormat = Bindings.createObjectBinding(Callable { getEncodedFormat() }, player.media.tracks)

    init {
        player.autoPlayProperty().bind(paused.not())
        paused.addListener { _, _ ,paused ->
            if (player.status == MediaPlayer.Status.PLAYING && paused) {
                player.pause()
            }
            player.play()
        }
        player.volumeProperty().bindBidirectional()
        player.muteProperty().bindBidirectional(mute)
        player.balanceProperty().bindBidirectional(balance)
        stopTime.bindBidirectional()
    }

    private fun getEncodedFormat(): AudioFormat? {
        val track = player.media.tracks.firstOrNull() ?: return null
        val encoding = if (track.metadata.containsKey("encoding")) {
            (track.metadata["encoding"] as String?)!!
        } else track.name
        return AudioFormat(AudioFormat.Encoding(encoding), -1.0f, -1, -1, -1, -1.0f, false, HashMap(track.metadata))
    }

    override fun setOutput(device: AudioDevice?) {
        if (device == null)
            player.stop()
        else {
            if (paused.value)
                player.pause()
            else
                player.play()
        }
    }

    override fun seek(position: Double) {
        player.value?.seek(Duration(position))
    }

    override fun dispose() {
        player.dispose()
    }
}


class FXBuffer(player: ObservableValue<MediaPlayer>, file: FXMediaFile) : AudioBuffer(file) {
    override val format: ObservableValue<AudioFormat>
        get() = TODO("Not yet implemented")
    override val inMemory: ObservableBooleanValue
        get() = TODO("Not yet implemented")
    override val growing: ObservableBooleanValue
        get() = TODO("Not yet implemented")
    override val allocatedMemory: ObservableObjectValue<Long?>
        get() = TODO("Not yet implemented")
    override val startPosition: ObservableDoubleValue
        get() = TODO("Not yet implemented")
    override val endPosition: ObservableDoubleValue
        get() = TODO("Not yet implemented")

}


private fun ObservableValue<Duration>.seconds(): ObservableValue<Double?> {
    return Bindings.createObjectBinding(Callable<Double?> { if (value.isIndefinite || value.isUnknown) null else value.toSeconds() }, this)
}