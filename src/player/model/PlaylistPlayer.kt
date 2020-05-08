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
import player.model.data.MasterGain
import player.model.data.PlayTask
import player.model.data.PlayTaskStatus
import player.model.data.Speaker
import java.io.File
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.function.Consumer
import java.util.function.Supplier
import kotlin.collections.ArrayList
import kotlin.collections.HashSet


/**
 * This data object specifies the state of a PlaylistPlayer.
 * It is sufficient to generate a Program.
 */
private class PlayerData
{

    data class Playlist(val files: List<CloudFile>) : SynchronizedData()

    data class Looping(val value: Boolean) : SynchronizedData()

    data class Shuffled(val value: Boolean) : SynchronizedData()

    data class Target(val value: Speaker?) : SynchronizedData()

    data class Paused(val value: Boolean) : SynchronizedData()

    /**
     * @param file start file of the last play request. Playback can move on to other files without this changing.
     * If null, nothing should be played.
     * @param position start position of the last play request
     */
    data class SelectedFile(val file: CloudFile?, val position: Double) : SynchronizedData() {
        /**
         * The ID changes when a new JumpRequest is made.
         * This will cause playback to jump to the position.
         */
        val id = UUID.randomUUID().toString()

        override fun toString(): String {
            return "JumpRequest(file=$file, position=$position, id='$id')"
        }
    }

}



/**
 * Issues tasks to the cloud, interprets results for simple displaying.
 */
class PlaylistPlayer(val cloud: Cloud, private val config: CycloneConfig) {
    val library: MediaLibrary = MediaLibrary()

    // Synchronized PlaylistPlayer data objects
    private val loopingData = cloud.getSynchronized(PlayerData.Looping::class.java, default = Supplier { PlayerData.Looping(config["looping"]?.toBoolean() ?: true) })
    private val shuffledData = cloud.getSynchronized(PlayerData.Shuffled::class.java, default = Supplier { PlayerData.Shuffled(config["shuffled"]?.toBoolean() ?: false) })
    private val gainData = cloud.getSynchronized(MasterGain::class.java, default = Supplier { MasterGain(config["gain"]?.toDouble() ?: 0.0) })
    private val playlistData = cloud.getSynchronized(PlayerData.Playlist::class.java, default = Supplier { PlayerData.Playlist(emptyList()) })
    private val speakerData = cloud.getSynchronized(PlayerData.Target::class.java, default = Supplier { PlayerData.Target(null) })
    private val pausedData = cloud.getSynchronized(PlayerData.Paused::class.java, default = Supplier { PlayerData.Paused(true) })
    private val jumpRequest = cloud.getSynchronized(PlayerData.SelectedFile::class.java, default = Supplier { PlayerData.SelectedFile(null, 0.0) })
    private val statuses = cloud.getAll(PlayTaskStatus::class.java)
    private val status = Bindings.createObjectBinding(Callable { statuses.firstOrNull {
        status -> status.task.creator == CREATOR && status.task.target == speakerData.value?.value } }, statuses)

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
    val playlist: ObservableList<CloudFile> = FXCollections.observableArrayList()
    val speakerProperty: ObjectProperty<Speaker?> = CustomObjectProperty<Speaker?>(listOf(speakerData),
            getter = Supplier { speakerData.value?.value },
            setter = Consumer { value -> cloud.pushSynchronized(PlayerData.Target(value)) })
    // Playback information - depend on status of (remote) PlaybackEngine(s)
    val speakers: ObservableList<Speaker> = cloud.getAll(Speaker::class.java)
    val currentFileProperty: ObjectProperty<CloudFile?> = CustomObjectProperty<CloudFile?>(listOf(status, jumpRequest),
            getter = Supplier<CloudFile?> { if(jumpRequest.value.file != null) status.value?.task?.file else null },
            setter = Consumer { value -> cloud.pushSynchronized(PlayerData.SelectedFile(value, 0.0)) })
    val positionProperty: CastToDoubleProperty = CastToDoubleProperty(CustomObjectProperty<Number?>(listOf(status),
            getter = Supplier<Number?> { status.value?.extrapolatePosition() ?: 0.0 },
            setter = Consumer { value -> cloud.pushSynchronized(PlayerData.SelectedFile(jumpRequest.value.file, value!!.toDouble())) }))
    // the durationProperty invalidation must come after positionProperty!
    // otherwise, the Slider may enforce position < duration and falsely set position
    val durationProperty: ReadOnlyDoubleProperty = CastToDoubleProperty(CustomObjectProperty<Number?>(listOf(status),
            getter = Supplier<Number?> { status.value?.task?.duration },
            setter = Consumer { throw UnsupportedOperationException() }))  // this is a read-only property
    val titleProperty: ReadOnlyStringProperty = CastToStringProperty(CustomObjectProperty<String>(listOf(status),
            getter = Supplier {
                status.value?.message()?.plus("") ?: AudioFiles.inferTitle(status.value?.task?.file?.getPath())
            },
            setter = Consumer { throw UnsupportedOperationException() }))  // this is a read-only property
    val playingProperty: BooleanProperty = CastToBooleanProperty(CustomObjectProperty<Boolean?>(listOf(pausedData),
            getter = Supplier<Boolean?> { pausedData.value?.value != true }, // status.value?.active == true && status.value?.task?.paused == false
            setter = Consumer { value -> cloud.pushSynchronized(PlayerData.Paused(!value!!)) }))
    val isFileSelectedProperty = Bindings.createBooleanBinding(Callable{ jumpRequest.value.file != null }, currentFileProperty)
    val playlistAvailableProperty = Bindings.createBooleanBinding(Callable{ playlist.size > 1 }, playlist)


    init {
        // --- Config ---
        if (config.properties.containsKey("library")) {
            val roots = config.getString("library", "").split(";".toRegex()).toTypedArray()
            for (root in roots) {
                if (!root.trim().isEmpty())
                    library.roots.add(CloudFile(File(root.trim())))
            }
        } else library.addDefaultRoots()

        playlistData.addListener(ChangeListener{ _, _, _ -> playlist.setAll(playlistData.value?.files ?: emptyList())}) // synchronized playlist

        for(obs in listOf(loopingData, shuffledData, playlistData, pausedData, jumpRequest, speakerData)) {
            obs.addListener { _ -> updateTasks() }
        }

        pickSpeaker()
        speakers.addListener { _: ListChangeListener.Change<out Speaker>? -> pickSpeaker()}


        Executors.newScheduledThreadPool(1).scheduleAtFixedRate({
            if (playingProperty.value) {
                Platform.runLater { (positionProperty.property as CustomObjectProperty).invalidate() }
            }
        }, 50, 50, TimeUnit.MILLISECONDS)

        status.addListener( InvalidationListener { if (status.value?.finished == true && speakerData.value.value?.peer?.isLocal == true) next() })
    }


    fun stop() {
        playingProperty.set(false)
        cloud.pushSynchronized(PlayerData.SelectedFile(currentFileProperty.value, 0.0))
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
        if (playlist.isEmpty()) return null
        if (currentFileProperty.value == null) return playlist[0]
        val index = playlist.indexOf(currentFileProperty.value)
        if (index < 0) return null
        return if (index < playlist.size - 1) playlist[index + 1]!! else if (loopingProperty.value) playlist[0] else null
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
            if(loopingProperty.get()) {
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
                speakerProperty.set(defaultSpeaker)
            }
        })
    }



    private val handledJumpRequests = HashSet<String>()
    private var activeTask: PlayTask? = null

    private fun updateTasks() {
        val speaker = speakerData.value.value
        if (speaker == null || !speaker.peer.isLocal) {
            cloud.yankAll(PlayTask::class.java, this)
            return
        }

        val file = jumpRequest.value.file  // not the actual file playing
        if (file == null) {
            cloud.yankAll(PlayTask::class.java, this)
            return
        }

        val shouldJump = jumpRequest.value.id !in handledJumpRequests
        if (shouldJump) handledJumpRequests.add(jumpRequest.value.id)

        val task: PlayTask
        val createNewTask = activeTask == null || file != status.value?.task?.file || status.value?.finished == true
        if (createNewTask) {
            task = PlayTask(speaker, file, 0.0, false, 0.0, jumpRequest.value.position, 0, null, CREATOR, pausedData.value.value, null, UUID.randomUUID().toString())
        }
        else {  // keep task ID
            val restartCount = if(shouldJump) activeTask!!.restartCount + 1 else activeTask!!.restartCount
            task = PlayTask(speaker, file, 0.0, false, 0.0, jumpRequest.value.position, restartCount, null, CREATOR, pausedData.value.value, null, activeTask!!.id)
        }

        activeTask = task
        cloud.push(PlayTask::class.java, listOf(task), this, true)
    }
}