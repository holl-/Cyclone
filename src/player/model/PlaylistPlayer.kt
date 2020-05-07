package player.model

import cloud.*
import javafx.application.Platform
import javafx.beans.binding.Bindings
import javafx.beans.binding.BooleanBinding
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

    data class JumpRequest(val file: CloudFile?, val position: Double?, val id: String = UUID.randomUUID().toString()) : SynchronizedData() {
        fun isRequest(): Boolean {
            return file != null || position != null
        }
    }

}



/**
 * Issues tasks to the cloud, interprets results for simple displaying.
 */
class PlaylistPlayer(val cloud: Cloud, private val config: CycloneConfig) {
    val library: MediaLibrary = MediaLibrary()
    private val handledJumpRequests = HashSet<String>()

    // Synchronized PlaylistPlayer data objects
    private val loopingData = cloud.getSynchronized(PlayerData.Looping::class.java, default = Supplier { PlayerData.Looping(config["looping"]?.toBoolean() ?: true) })
    private val shuffledData = cloud.getSynchronized(PlayerData.Shuffled::class.java, default = Supplier { PlayerData.Shuffled(config["shuffled"]?.toBoolean() ?: false) })
    private val gainData = cloud.getSynchronized(MasterGain::class.java, default = Supplier { MasterGain(config["gain"]?.toDouble() ?: 0.0) })
    private val playlistData = cloud.getSynchronized(PlayerData.Playlist::class.java, default = Supplier { PlayerData.Playlist(emptyList()) })
    private val speakerData = cloud.getSynchronized(PlayerData.Target::class.java, default = Supplier { PlayerData.Target(null) })
    private val pausedData = cloud.getSynchronized(PlayerData.Paused::class.java, default = Supplier { PlayerData.Paused(false) })
    private val jumpRequest = cloud.getSynchronized(PlayerData.JumpRequest::class.java, default = Supplier { PlayerData.JumpRequest(null, null) })
    private val statuses = cloud.getAll(PlayTaskStatus::class.java)
    private val status = Bindings.createObjectBinding(Callable { statuses.firstOrNull {
        status -> status.task.creator == CREATOR && status.task.target == speakerData.value?.value } }, statuses)


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
    val currentFileProperty: ObjectProperty<CloudFile?> = CustomObjectProperty<CloudFile?>(listOf(status),
            getter = Supplier<CloudFile?> { status.value?.task?.file },
            setter = Consumer { value -> cloud.pushSynchronized(PlayerData.JumpRequest(value, 0.0)) })
    val durationProperty: ReadOnlyDoubleProperty = CastToDoubleProperty(CustomObjectProperty<Number?>(listOf(status),
            getter = Supplier<Number?> { status.value?.task?.duration },
            setter = Consumer { throw UnsupportedOperationException() }))  // this is a read-only property
    val positionProperty: CastToDoubleProperty = CastToDoubleProperty(CustomObjectProperty<Number?>(listOf(status),
            getter = Supplier<Number?> { status.value?.extrapolatePosition() ?: 0.0 },
            setter = Consumer { value -> cloud.pushSynchronized(PlayerData.JumpRequest(null, value!!.toDouble())) }))
    val titleProperty: ReadOnlyStringProperty = CastToStringProperty(CustomObjectProperty<String>(listOf(status),
            getter = Supplier {
                status.value?.message()?.plus("") ?: AudioFiles.inferTitle(status.value?.task?.file?.getPath())
            },
            setter = Consumer { throw UnsupportedOperationException() }))  // this is a read-only property
    val playingProperty: BooleanProperty = CastToBooleanProperty(CustomObjectProperty<Boolean?>(listOf(status),
            getter = Supplier<Boolean?> { pausedData.value?.value != true }, // status.value?.active == true && status.value?.task?.paused == false
            setter = Consumer { value -> cloud.pushSynchronized(PlayerData.Paused(!value!!)) }))
    val isFileSelectedProperty: BooleanBinding = Bindings.createBooleanBinding(Callable{ currentFileProperty.get() != null }, currentFileProperty)


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
    }


    fun stop() {
        playingProperty.set(false)
        cloud.pushSynchronized(PlayerData.JumpRequest(currentFileProperty.value, 0.0))
    }

    fun addToPlaylist(files: List<CloudFile>) {
        val newList = ArrayList(playlist)
        newList.addAll(if(shuffledProperty.value) files.shuffled() else files)
        setPlaylist(newList)
    }

    fun setPlaylist(files: List<CloudFile>) {
        cloud.pushSynchronized(PlayerData.Playlist(if(shuffledProperty.value) files.shuffled() else files))
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


    private fun updateTasks() {
        if(speakerProperty.value?.peer?.isLocal != true) {
            cloud.yankAll(PlayTask::class.java, this)
            return
        }
        val tasks = ArrayList<PlayTask>()
        if(jumpRequest.value.isRequest() && jumpRequest.value!!.id !in handledJumpRequests) {
            handledJumpRequests.add(jumpRequest.value.id)
            if(jumpRequest.value.file == null) {
                // only jump
                println("Position jump???")
                return
            }
            val file = jumpRequest.value.file!!
            val task = PlayTask(speakerData.value.value!!, file, gainData.value.value, false, 0.0, 0.0, 0, null, CREATOR, pausedData.value?.value == true, null, UUID.randomUUID().toString())
            tasks.add(task)
        }
        if(currentFileProperty.value != null) {
            val file = currentFileProperty.get()!!
            // TODO modify task
            val existingTask = cloud.getAll(PlayTask::class.java).first { task -> task.file == file }
            val task = PlayTask(speakerData.value.value!!, file, gainData.value.value, false, 0.0, 0.0, existingTask.restartCount, null, CREATOR, pausedData.value?.value == true, null, existingTask.id)
            tasks.add(task)
        }
        println("Sending tasks $tasks")
        cloud.push(PlayTask::class.java, tasks, this, true)
    }


    private fun pickSpeaker() {
        Platform.runLater(Runnable {
            if(speakerProperty.value == null && !speakers.isEmpty()) {
                val defaultSpeaker = speakers.firstOrNull { s -> s.isDefault } ?: speakers[0]
                speakerProperty.set(defaultSpeaker)
            }
        })
    }


    companion object {
        private const val CREATOR = "PlaylistPlayer"
    }
}