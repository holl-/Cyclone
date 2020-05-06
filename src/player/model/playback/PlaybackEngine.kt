package player.model.playback

import audio.*
import audio.javafx.JavaFXAudioEngine
import audio.javasound.JavaSoundEngine
import cloud.Cloud
import cloud.Peer.Companion.getLocal
import javafx.collections.ListChangeListener
import player.model.data.PlayTask
import player.model.data.Speaker
import player.model.data.Task
import player.model.data.PlayTaskStatus
import java.util.stream.Collectors

/**
 * Controls the isLocal audio engine.
 * An instance of PlaybackEngine observe a PlayerStatus.
 * Whenever an audio file should be played back on a isLocal device, the PlaybackEngine loads the file and plays it back.
 * It also adjusts position and gain to match the desired status.
 *
 * When a playback event occurs like an error or end-of-file, the PlaybackEngine updates the status.
 */
class PlaybackEngine private constructor(val cloud: Cloud, private val audioEngine: AudioEngine)
{
    private val tasks = cloud.getAll(Task::class.java)

    /**
     * Stores created players for existing and future tasks.
     * Finished tasks map to null and are removed once the program does not reference them anymore.
     */
    private val players = HashMap<PlayTask, Player?>()

    private val speakers: Map<Speaker, AudioDevice> = audioEngine.devices.stream().collect(Collectors.toMap({ dev -> Speaker(getLocal(), dev.id, dev.name, dev.minGain, dev.maxGain, dev.isDefault) }, { dev -> dev}))

    init {
//        supportedTypes = ArrayList(audio.supportedMediaTypes.stream().map { t: MediaType -> t.fileExtension }.collect(Collectors.toList()))
        cloud.push(Speaker::class.java, speakers.keys, this, true)
        tasksUpdated()
        tasks.addListener(ListChangeListener<Task> { tasksUpdated() })
    }


    companion object {
        @JvmStatic
        @Throws(AudioEngineException::class)
        fun initializeAudioEngine(cloud: Cloud, engineName: String?): PlaybackEngine {
            val engine: AudioEngine = if (engineName == null) JavaSoundEngine() else if (engineName == "java") JavaSoundEngine() else if (engineName == "javafx") JavaFXAudioEngine() else throw AudioEngineException("No audio engine registered for name $engineName")
            return PlaybackEngine(cloud, engine)
        }
    }


    private fun tasksUpdated() {
        for (task in tasks) {
            if (task is PlayTask && task.target in speakers.keys) { // Check if we are responsible for this task queue
                println(task)
                val player = if (task in players.keys) players[task] else createPlayer(task)
                player?.gain = task.gain
                player?.isMute = task.mute
                player?.balance = task.balance
                if (!task.paused)
                    player?.start()
                else player?.pause()
            }
        }
        for (oldTask in players.keys) {
            if(oldTask !in tasks) {
                players[oldTask]?.dispose()
                players.remove(oldTask)
            }
        }
        publishInfo()
    }


    /**
     * Updates the PlaybackStatus to reflect the current status of the audio engine.
     * This may result in the information being sent to connected machines.
     */
    private fun publishInfo() {
        val statuses = ArrayList<PlayTaskStatus>()
        for(entry in players) {
            val player = entry.value
            if(player != null) {
                val task = entry.key
                val expandedTask = PlayTask(task.target, task.file, player.gain, player.isMute, player.balance, player.position, player.duration, task.creator, task.paused, emptyList(), task)
                statuses.add(PlayTaskStatus(expandedTask, true, null, null, System.currentTimeMillis()))
            }
        }
        cloud.push(PlayTaskStatus::class.java, statuses, this, true)
    }


    private fun createPlayer(task: PlayTask): Player? {  // TODo error handling
        val file: MediaFile = DMediaFile(task.file)
        try {
            val player = audioEngine.newPlayer(file)
            players[task] = player
            player.prepare()
            player.activate(speakers[task.target])
            player.addEndOfMediaListener { e -> task.onFinished }  // TODO start follow-up tasks
            if (player.getDuration() < 0) {
                Thread {
                    try {
                        player.waitForDurationProperty()
                        publishInfo()
                    } catch (e1: IllegalStateException) {
                        e1.printStackTrace()
                    } catch (e1: InterruptedException) {}
                }.start()
            }
            return player
        } catch (exc: Exception) {
            exc.printStackTrace()
            return null
        }
    }

    fun dispose() {
        cloud.yankAll(null, this)
        for(player in players.values) {
            player?.dispose()
        }
    }


}