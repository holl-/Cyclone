package player.model.playback

import audio.MediaFile
import audio.Player
import javafx.beans.InvalidationListener
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import player.model.data.PlayTask
import player.model.data.PlayTaskStatus
import player.model.data.Speaker
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

class Job(val taskId: String, val engine: PlaybackEngine, val bufferTime: Double) {
    var task = SimpleObjectProperty<PlayTask?>()  // alive when not null
    private val restartCount = SimpleIntegerProperty(-1)

    var player = SimpleObjectProperty<Player?>()  // inactive when not null
    private var creatingPlayer = false

    val started = SimpleBooleanProperty(false)
    val finished = SimpleBooleanProperty(false)
    val errorMessage = SimpleStringProperty(null)
    val busyMessage = SimpleStringProperty(null)
    val previous = SimpleObjectProperty<Job?>()

    val status = SimpleObjectProperty<PlayTaskStatus?>()


    init {
        task.addListener(InvalidationListener { update() })
        previous.addListener { _, _, value ->
            value?.started?.addListener { _, _, _ -> update() }
            value?.finished?.addListener { _, _, _ -> update() }
        }
        finished.addListener(InvalidationListener { engine.mainThread.submit { status.value = status() } })
        engine.masterGain.addListener(InvalidationListener { player.value?.gain = task.value?.let { it1 -> mixGain(it1) } ?: 0.0 })
    }


    private fun update() {
        val task = this.task.value ?: run {
            dispose()
            return
        }

        if (task.trigger != null && previous.value == null) {
            previous.value = engine.getOrCreateJob(task.trigger.taskId)
        }
        if (task.trigger == null && previous.value != null) {
            previous.value = null
        }

        if (!started.value) started.value = checkTriggerCondition()
        if(checkPrepareCondition()) {
            if (player.value == null && !creatingPlayer && errorMessage.value == null) {
                creatingPlayer = true
                engine.jobThreads.submit {
                    val player = createPlayer()
                    engine.mainThread.submit { this.player.value = player; creatingPlayer = false; update() }
                }
            }
            player.value?.let { pl -> adjustPlayer(pl, task) }
        }  // else destroy player?

        engine.mainThread.submit { status.value = status() }

        updateEndListener()
    }

    private fun remaining(): Double? {
        if (finished.value) return 0.0
        val task = this.task.value ?: return null
        val player = this.player.value ?: return task.duration
        val duration = task.duration ?: player.duration
        if (duration < 0) return null
        return duration + task.position - player.position
    }

    private fun checkTriggerCondition(): Boolean {
        val task = this.task.value ?: return false
        if (task.trigger == null) return true
        val waitingOnJob = previous.value ?: return true
        return if (waitingOnJob.isAlive()) waitingOnJob.finished.value else false
    }

    private fun checkPrepareCondition(): Boolean {
        val task = this.task.value ?: return false
        if (task.trigger == null) return true
        val waitingOnJob = previous.value ?: return true
        if (waitingOnJob.finished.value) return true
        return if (waitingOnJob.isAlive()) waitingOnJob.started.value else false
    }


    private var target: Speaker? = null
    /**
     * While this job is alive, its status can be queued.
     */
    private fun status(): PlayTaskStatus? {
        val task = this.task.value ?: return null
        if (task.target in engine.speakerMap)
            target = task.target
        val player = this.player.value
        val gain = player?.gain ?: mixGain(task)
        val mute = player?.isMute ?: task.mute
        val balance = player?.balance ?: task.balance
        val position = player?.position ?: task.position
        val duration = player?.duration ?: task.duration
        val paused = task.paused && player?.isPlaying == false
        val expandedTask = PlayTask(target!!, task.file, gain, mute, balance, position, restartCount.value, duration, task.creator, paused, task.trigger, task.id)
        val active = player != null && started.value && !finished.value
        return PlayTaskStatus(expandedTask, active, finished.value, busyMessage.value, errorMessage.value, System.currentTimeMillis(), true)
    }


    private fun createPlayer(): Player? {
        val file = engine.files.get(task.value!!.file)
        busyMessage.value = "Loading '$file'"
        try {
            val player = engine.audioEngine.newPlayer(file)
            player.prepare()
            simulateWait()
            player.addEndOfMediaListener { finished.value = true; }
            if (player.getDuration() < 0) {
                Thread {
                    try {
                        player.waitForDurationProperty()
                        simulateWait()
                        status.value = status()
                        updateEndListener()
                    } catch (exc: IllegalStateException) {
                        errorMessage.value = "${exc.javaClass}: ${exc.message}"
                        exc.printStackTrace()
                    } catch (_: InterruptedException) {}
                }.start()
            }
            errorMessage.value = null
            busyMessage.value = null
            return player
        } catch (exc: Exception) {
            errorMessage.value = "${exc.javaClass}: ${exc.message}"
            exc.printStackTrace()
            return null
        }
    }

    private fun adjustPlayer(player: Player, task: PlayTask) {
        try {
            val targetDevice = engine.speakerMap[task.target]
            if (targetDevice == null) player.deactivate()
            else {
                if(player.device != null) player.switchDevice(targetDevice, bufferTime)
                else player.activate(targetDevice, bufferTime)
            }
            if(task.restartCount > restartCount.value) {
                if (task.position >= 0)
                    player.setPositionAsync(task.position) { engine.mainThread.submit { status.value = status(); updateEndListener() } }
                restartCount.value = task.restartCount
            }
            if (targetDevice != null) {
                player.gain = mixGain(task)
                player.isMute = task.mute
                player.balance = task.balance
                val shouldPlay = !task.paused && started.value && !finished.value
                if (shouldPlay) player.start() else player.pause()
            }
        } catch (exc: Exception) {
            exc.printStackTrace()
            errorMessage.value = "${exc.javaClass}: ${exc.message}"
        }
    }

    /**
     * Dead jobs are removed from the job list.
     * They will be disposed of as soon as no other job references them anymore.
     */
    fun isAlive(): Boolean {
        this.task.value ?: return false
        return true
    }

    fun references(): List<Job> {
        if (started.value) return emptyList()
        return listOfNotNull(previous.value)
    }

    fun dispose() {
        player.value?.dispose()
        player.value = null
        task.value = null
    }

    override fun toString(): String {
        return "Job(${taskId}, alive=${isAlive()})"
    }

    private fun mixGain(task: PlayTask): Double {
        return task.gain + engine.masterGain.value
    }

    private var end: ScheduledFuture<*>? = null
    private val endPool = Executors.newScheduledThreadPool(1)

    private fun updateEndListener() {
        end?.cancel(false)
        val task = this.task.value ?: return
        if (player.value?.isPlaying == true && task.duration != null) {
            val remaining = remaining()
            if (remaining != null) {
                end = endPool.schedule({
                    if (remaining() ?: 1.0 < 0.0) {
                        player.value?.pause()
                        finished.value = true
                    } else {
                        updateEndListener()
                    }
                }, (remaining * 1000).toLong() + 1, TimeUnit.MILLISECONDS)
            }
        }
    }
}