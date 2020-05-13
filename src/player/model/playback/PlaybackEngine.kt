package player.model.playback

import audio.AudioDevice
import audio.AudioEngine
import audio.javafx.JavaFXAudioEngine
import audio.javasound.JavaSoundEngine
import cloud.Cloud
import cloud.Peer.Companion.getLocal
import javafx.beans.InvalidationListener
import javafx.beans.binding.Bindings
import javafx.beans.property.SimpleBooleanProperty
import javafx.collections.FXCollections
import javafx.collections.ListChangeListener
import player.model.CycloneConfig
import player.model.data.MasterGain
import player.model.data.PlayTask
import player.model.data.PlayTaskStatus
import player.model.data.Speaker
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.stream.Collectors

/**
 * Controls the isLocal audio engine.
 * An instance of PlaybackEngine observe a PlayerStatus.
 * Whenever an audio file should be played back on a isLocal device, the PlaybackEngine loads the file and plays it back.
 * It also adjusts position and gain to match the desired status.
 *
 * When a playback event occurs like an error or end-of-file, the PlaybackEngine updates the status.
 */
class PlaybackEngine (val cloud: Cloud, val config: CycloneConfig)
{
    val audioEngine: AudioEngine = createEngine(config.audioEngine.value)
    val mainThread = Executors.newFixedThreadPool(1)
    val jobThreads = Executors.newFixedThreadPool(2)

    private val tasks = cloud.getAll(PlayTask::class.java, this) { r -> mainThread.submit(r) }
    private val masterGainData = cloud.getSynchronized(MasterGain::class.java) { r -> mainThread.submit(r) }

    // Public properties
    val jobs = FXCollections.observableArrayList<Job>()
    val masterGain = Bindings.createDoubleBinding(Callable { masterGainData.value.value }, masterGainData)
    val statusInvalid = SimpleBooleanProperty()

    val speakerMap: Map<Speaker, AudioDevice> = audioEngine.devices.stream().collect(Collectors.toMap({ dev -> Speaker(getLocal(), dev.id, dev.name, dev.minGain, dev.maxGain, dev.isDefault) }, { dev -> dev}))


    init {
//        supportedTypes = ArrayList(audio.supportedMediaTypes.stream().map { t: MediaType -> t.fileExtension }.collect(Collectors.toList()))
        cloud.push(Speaker::class.java, speakerMap.keys, this, true)
        mainThread.submit { tasksUpdated() }
        tasks.addListener(ListChangeListener { mainThread.submit { tasksUpdated() } })
        statusInvalid.addListener(InvalidationListener { if(statusInvalid.value == true) publishInfo() })
    }


    private fun tasksUpdated() {
        // --- Handle existing tasks ---
        for (task in tasks) {
            var job = getJob(task.id)
            if (job == null) {
                if (task.target in speakerMap.keys) {
                    job = getOrCreateJob(task.id)
                }
            }
            job?.task?.value = task
        }
        // --- Handle deleted tasks ---
        for (job in jobs) {
            if (job.task.value !in tasks) {
                job.task.value = null
            }
        }
        // --- Delete jobs ---
        for (job in ArrayList(jobs)) {
            if (!job.isAlive()) {
                if (jobs.none { j -> job in j.references() }) {
                    jobs.remove(job)
                }
            }
        }
    }


    fun getJob(taskId: String): Job? {
        return jobs.firstOrNull { j -> j.taskId == taskId }
    }


    fun getOrCreateJob(taskId: String): Job {
        val existing = jobs.firstOrNull { j -> j.taskId == taskId}
        existing?.let { return existing }
        val newJob = Job(taskId, this, config.bufferTime.value)
        jobs.add(newJob)
        newJob.status.addListener(InvalidationListener { statusInvalid.value = true })
        return newJob
    }


    /**
     * Updates the PlaybackStatus to reflect the current status of the audio engine.
     * This may result in the information being sent to connected machines.
     *
     * Called when [statusInvalid] is set.
     */
    private fun publishInfo() {
        statusInvalid.value = false

        val statuses = ArrayList<PlayTaskStatus>()
        for(job in ArrayList(jobs)) {
            val status = job.status.value
            status?.let { statuses.add(status) }
        }
        cloud.push(PlayTaskStatus::class.java, statuses, this, true)
    }


    fun dispose() {
        jobs.forEach { job -> job.dispose() }
        cloud.yankAll(null, this)
    }

    private fun createEngine(name: String?): AudioEngine {
        if (name == "javafx") {
            return JavaFXAudioEngine()
        }
        if (name == null || name == "java") {
            return JavaSoundEngine()
        }
        throw IllegalArgumentException("Unknown audio engine: $name")
    }

}