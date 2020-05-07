package player.model.playback

import audio.*
import audio.javafx.JavaFXAudioEngine
import audio.javasound.JavaSoundEngine
import cloud.Cloud
import cloud.Peer.Companion.getLocal
import javafx.collections.FXCollections
import javafx.collections.ListChangeListener
import player.model.data.PlayTask
import player.model.data.Speaker
import player.model.data.PlayTaskStatus
import java.util.function.Consumer
import java.util.stream.Collectors

/**
 * Controls the isLocal audio engine.
 * An instance of PlaybackEngine observe a PlayerStatus.
 * Whenever an audio file should be played back on a isLocal device, the PlaybackEngine loads the file and plays it back.
 * It also adjusts position and gain to match the desired status.
 *
 * When a playback event occurs like an error or end-of-file, the PlaybackEngine updates the status.
 */
class PlaybackEngine private constructor(val cloud: Cloud, val audioEngine: AudioEngine)
{
    private val tasks = cloud.getAll(PlayTask::class.java)
    private val jobs = FXCollections.observableArrayList<Job>()

    val speakerMap: Map<Speaker, AudioDevice> = audioEngine.devices.stream().collect(Collectors.toMap({ dev -> Speaker(getLocal(), dev.id, dev.name, dev.minGain, dev.maxGain, dev.isDefault) }, { dev -> dev}))

    init {
//        supportedTypes = ArrayList(audio.supportedMediaTypes.stream().map { t: MediaType -> t.fileExtension }.collect(Collectors.toList()))
        cloud.push(Speaker::class.java, speakerMap.keys, this, true)
        tasksUpdated()
        tasks.addListener(ListChangeListener<PlayTask> { tasksUpdated() })
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
        // --- Handle existing tasks ---
        for (task in tasks) {
            var job = this[task]
            if (job == null) {
                if (task.target in speakerMap.keys) {
                    job = Job(this)
                    job.eventListeners.add(Consumer { publishInfo() })
                    jobs.add(job)
                }
            }
            job?.taskUpdated(task)
        }
        // --- Handle deleted tasks ---
        for (job in jobs) {
            if (job.task !in tasks) {
                job.taskDeleted()
            }
        }
        // --- Delete jobs ---
        for (job in ArrayList(jobs)) {
            if (!job.isAlive()) {
                jobs.remove(job)
            }
        }
        publishInfo()
    }


    operator fun get(task: PlayTask): Job? {
        return jobs.firstOrNull { j -> j.task == task }
    }

    operator fun get(taskId: String): Job? {
        return jobs.firstOrNull { j -> j.task?.id == taskId}
    }


    /**
     * Updates the PlaybackStatus to reflect the current status of the audio engine.
     * This may result in the information being sent to connected machines.
     */
    private fun publishInfo() {
        val statuses = ArrayList<PlayTaskStatus>()
        for(job in jobs) {
            if (job.isAlive()) {
                statuses.add(job.status())
            }
        }
        cloud.push(PlayTaskStatus::class.java, statuses, this, true)
    }


    fun dispose() {
        jobs.forEach { job -> job.dispose() }
        cloud.yankAll(null, this)
    }


}