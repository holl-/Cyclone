package player.model

import cloud.*
import javafx.application.Platform
import javafx.beans.InvalidationListener
import javafx.beans.binding.Bindings
import javafx.beans.property.*
import javafx.beans.value.ChangeListener
import javafx.collections.FXCollections
import javafx.collections.ListChangeListener
import javafx.collections.ObservableList
import player.CastToBooleanProperty
import player.CastToDoubleProperty
import player.CastToStringProperty
import player.CustomObjectProperty
import player.model.data.*
import java.io.File
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.function.Consumer
import java.util.function.Function
import java.util.function.Supplier
import kotlin.collections.ArrayList
import kotlin.math.abs


/**
 * This data object specifies the state of a PlaylistPlayer.
 * It is sufficient to generate a Program.
 */
class PlayerData
{

    data class Playlist(val files: List<CloudFile>) : SynchronizedData()
    {
        constructor() : this(emptyList())

        override fun resolveConflict(other: SynchronizedData): SynchronizedData {
            return if (files.isEmpty()) other else this
        }

        override fun fromFile(): SynchronizedData? {
            return Playlist(files.filter { f -> f.originatesHere() })
        }
    }

    data class Looping(val value: Boolean) : SynchronizedData()
    {
        constructor() : this(true)
    }

    data class Shuffled(val value: Boolean) : SynchronizedData()
    {
        constructor() : this(false)
    }

    data class Target(val value: Speaker?) : SynchronizedData()
    {
        constructor() : this(null)

        override fun resolveConflict(other: SynchronizedData): SynchronizedData {
            return if (value == null) other else this
        }

        override fun fromFile(): SynchronizedData? {
            return Target(null)
        }
    }

    data class Paused(val value: Boolean) : SynchronizedData()
    {
        constructor() : this(true)

        override fun fromFile(): SynchronizedData? {
            return Paused(true)
        }
    }

    /**
     * @param file start file of the last play request. Playback can move on to other files without this changing.
     * If null, nothing should be played.
     * @param position start position of the last play request
     * @param jumpCount whenever this value is incremented, the player should jump to the specified location.
     * As long as this value stays constant, any changes are interpreted as updates with no actions required.
     */
    data class SelectedFile(val file: CloudFile?, val position: Double, val jumpCount: Long) : SynchronizedData()
    {
        constructor() : this(null, 0.0, 0)

        override fun fromFile(): SynchronizedData? {
            return if (file != null && file.originatesHere() && File(file.getPath()).exists()) {
                SelectedFile(file, 0.0, 0)
            } else {
                SelectedFile(null, 0.0, 0)
            }
        }
    }

}



/**
 * Issues tasks to the cloud, interprets results for simple displaying.
 */
class PlaylistPlayer(val cloud: Cloud, private val config: CycloneConfig) {
    val library: MediaLibrary = MediaLibrary()
    private var jumpCount: Long = 0
    private val builder = TaskChainBuilder(cloud, Function { file -> after(file) }, CREATOR)

    // Synchronized PlaylistPlayer data objects
    private val loopingData = cloud.getSynchronized(PlayerData.Looping::class.java, Platform::runLater)
    private val shuffledData = cloud.getSynchronized(PlayerData.Shuffled::class.java, Platform::runLater)
    private val gainData = cloud.getSynchronized(MasterGain::class.java, Platform::runLater)
    private val playlistData = cloud.getSynchronized(PlayerData.Playlist::class.java, Platform::runLater)
    private val speakerData = cloud.getSynchronized(PlayerData.Target::class.java, Platform::runLater)
    private val pausedData = cloud.getSynchronized(PlayerData.Paused::class.java, Platform::runLater)
    private val selectedFile = cloud.getSynchronized(PlayerData.SelectedFile::class.java, Platform::runLater)
    private val statuses = cloud.getAll(PlayTaskStatus::class.java, this, Platform::runLater)
    private val tasks = cloud.getAll(PlayTask::class.java, this, Platform::runLater)
    private val status = Bindings.createObjectBinding(Callable { getStatus() }, statuses, speakerData, tasks)


    companion object {
        private const val CREATOR = "PP"
    }


    // Controls - synchronized data as nested classes of PlayerData
    val loopingProperty: BooleanProperty = CastToBooleanProperty(CustomObjectProperty<Boolean?>(listOf(loopingData),
            getter = Supplier { loopingData.value?.value ?: false },
            setter = Consumer { value -> cloud.pushSynchronized(PlayerData.Looping(value!!)) }))
    val shuffledProperty: BooleanProperty = CastToBooleanProperty(CustomObjectProperty<Boolean?>(listOf(shuffledData),
            getter = Supplier { shuffledData.value?.value ?: false },
            setter = Consumer { value -> cloud.pushSynchronized(PlayerData.Shuffled(value!!)) }))
    val gainProperty: DoubleProperty = CastToDoubleProperty(CustomObjectProperty<Number?>(listOf(gainData),
            getter = Supplier<Number?> { gainData.value?.value ?: 0 },
            setter = Consumer { value -> cloud.pushSynchronized(MasterGain(value!!.toDouble())) }))
    val playlist: ObservableList<CloudFile> = FXCollections.observableArrayList(playlistData.value.files)
    val speakerProperty: ObjectProperty<Speaker?> = CustomObjectProperty<Speaker?>(listOf(speakerData),
            getter = Supplier { speakerData.value?.value },
            setter = Consumer { value -> updateSelectedFile(false); cloud.pushSynchronized(PlayerData.Target(value)) })
    // Playback information - depend on status of (remote) PlaybackEngine(s)
    val speakers: ObservableList<Speaker> = cloud.getAll(Speaker::class.java, this, Platform::runLater)
    val currentFileProperty: ObjectProperty<CloudFile?> = CustomObjectProperty<CloudFile?>(listOf(status, selectedFile),
            getter = Supplier<CloudFile?> { if(selectedFile.value.file != null) status.value?.task?.file else null },
            setter = Consumer { value -> cloud.pushSynchronized(PlayerData.SelectedFile(value, 0.0, selectedFile.value.jumpCount + 1)) })
    val positionProperty: CastToDoubleProperty = CastToDoubleProperty(CustomObjectProperty<Number?>(listOf(status),
            getter = Supplier<Number?> { status.value?.extrapolatePosition() ?: 0.0 },
            setter = Consumer { value -> cloud.pushSynchronized(PlayerData.SelectedFile(selectedFile.value.file, value!!.toDouble(), selectedFile.value.jumpCount + 1)) }))
    // the durationProperty invalidation must come after positionProperty!
    // otherwise, the Slider may enforce position < duration and falsely set position
    val durationProperty: ReadOnlyDoubleProperty = CastToDoubleProperty(CustomObjectProperty<Number?>(listOf(status),
            getter = Supplier<Number?> { status.value?.task?.duration },
            setter = Consumer { throw UnsupportedOperationException() }))  // this is a read-only property
    val titleProperty: ReadOnlyStringProperty = CastToStringProperty(CustomObjectProperty<String>(listOf(status),
            getter = Supplier {
                status.value?.message() ?: AudioFiles.inferTitle(status.value?.task?.file?.getPath())
            },
            setter = Consumer { throw UnsupportedOperationException() }))  // this is a read-only property
    val playingProperty: BooleanProperty = CastToBooleanProperty(CustomObjectProperty<Boolean?>(listOf(pausedData),
            getter = Supplier<Boolean?> { pausedData.value?.value != true }, // status.value?.active == true && status.value?.task?.paused == false
            setter = Consumer { value -> cloud.pushSynchronized(PlayerData.Paused(!value!!)) }))
    val isFileSelectedProperty = Bindings.createBooleanBinding(Callable{ selectedFile.value.file != null }, currentFileProperty)
    val playlistAvailableProperty = Bindings.createBooleanBinding(Callable{ playlist.size > 1 }, playlist)


    init {
        library.roots.addAll(config.getLibraryFiles())
        library.roots.addListener(ListChangeListener<CloudFile> { config.setLibraryFiles(library.roots) })

        playlistData.addListener(ChangeListener{ _, _, _ -> playlist.setAll(playlistData.value?.files ?: emptyList())})

        for(obs in listOf(loopingData, shuffledData, playlistData, pausedData, selectedFile, speakerData)) {
            obs.addListener { _ -> updateTasks() }
        }

        pickSpeaker()
        speakers.addListener { _: ListChangeListener.Change<out Speaker>? -> pickSpeaker()}


        Executors.newScheduledThreadPool(1).scheduleAtFixedRate({
            if (playingProperty.value) {
                Platform.runLater { (positionProperty.property as CustomObjectProperty).invalidate() }
            }
        }, 50, 50, TimeUnit.MILLISECONDS)

        status.addListener( InvalidationListener { updateSelectedFile() })
    }


    fun stop() {
        playingProperty.set(false)
        positionProperty.set(0.0)
    }

    fun addToPlaylist(files: List<CloudFile>) {
        val newList = ArrayList(playlist)
        newList.addAll(if(shuffledProperty.value) files.shuffled() else files)
        setPlaylist(newList)
    }

    fun setPlaylist(files: List<CloudFile>) {
        cloud.pushSynchronized(PlayerData.Playlist(if(shuffledProperty.value) files.shuffled() else files))
        playingProperty.set(true);
    }

    fun getNext(): CloudFile? {
        return after(currentFileProperty.value)
    }

    fun after(file: CloudFile?): CloudFile? {
        if (playlist.isEmpty()) return null
        if (file == null) return playlist[0]
        val index = playlist.indexOf(file)
        if (index < 0) return null
        return if (index < playlist.size - 1) playlist[index + 1]!! else if (loopingData.value.value) playlist[0] else null
    }

    fun getPrevious(): CloudFile? {
        if (playlist.isEmpty()) return null
        if (currentFileProperty.value == null) return playlist[playlist.size - 1]
        val index = playlist.indexOf(currentFileProperty.value)
        if (index < 0) return null
        return if (index > 0) playlist[index -1] else if (loopingProperty.value) playlist[playlist.size - 1] else null
    }

    operator fun next() {
        currentFileProperty.set(getNext())
    }

    fun previous() {
        currentFileProperty.set(getPrevious())
    }

    fun removeCurrentFileFromPlaylist() {
        if (currentFileProperty.get() == null)
            return
        val index = playlist.indexOf(currentFileProperty.get())
        playlist.remove(currentFileProperty.get())
        if(playlist.size > index) {
            currentFileProperty.set(playlist[index])
        } else {
            if(loopingProperty.get() && playlist.isNotEmpty()) {
                currentFileProperty.set(playlist[0])
            } else {
                currentFileProperty.set(null)
            }
        }
    }

    private fun pickSpeaker() {
        Platform.runLater(Runnable {
            if(speakerProperty.value == null && !speakers.isEmpty()) {
                val defaultSpeaker = speakers.firstOrNull { s -> s.isDefault } ?: speakers[0]
                speakerProperty.value = defaultSpeaker
            }
        })
    }


    private fun updateTasks() {
        val speaker = speakerData.value.value
        val file = selectedFile.value.file  // not the actual file playing

        if (speaker != null && speaker.peer.isLocal && file != null) {
            builder.activate(speaker)
            if (selectedFile.value.jumpCount > jumpCount || !builder.isPlaying()) {
                jumpCount = selectedFile.value.jumpCount
                builder.play(file, selectedFile.value.position)
            } else {
                builder.update()
            }
            builder.paused.value = pausedData.value.value
        }
        else {
            builder.deactivate()
        }
    }


    private fun getStatus(): PlayTaskStatus? {
        val scheduled = statuses.filter { status -> status.task.creator == CREATOR && status.task.target == speakerData.value?.value && status.task in tasks }
        scheduled.firstOrNull { status -> status.active }?.let { status -> return status }
        val result = scheduled.firstOrNull()
//        if (result == null)
//            println("No status")
        return result
    }

    private fun updateSelectedFile(onlyIfOwner: Boolean = true) {
        val status = status.value ?: return
        if (speakerData.value?.value?.peer?.isLocal != true && onlyIfOwner) return  // only playing peer should update
        val shouldUpdate = status.task.file != selectedFile.value.file || abs(status.extrapolatePosition() - selectedFile.value.position) > 1.0
        if (shouldUpdate) {
            cloud.pushSynchronized(PlayerData.SelectedFile(status.task.file, status.extrapolatePosition(), selectedFile.value.jumpCount))
        }
    }
}