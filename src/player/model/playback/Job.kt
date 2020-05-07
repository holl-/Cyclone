package player.model.playback

import audio.AudioDevice
import audio.AudioEngine
import audio.MediaFile
import audio.Player
import javafx.beans.InvalidationListener
import player.model.data.PlayTask
import player.model.data.PlayTaskStatus
import player.model.data.Speaker
import java.util.concurrent.CopyOnWriteArrayList
import java.util.function.Consumer

class Job(val engine: PlaybackEngine) {
    var task: PlayTask? = null  // alive when not null
    var player: Player? = null  // inactive when not null
    private var errorMessage: String? = null
    private var busyMessage: String? = null
    private var finished = false
    val eventListeners: MutableList<Consumer<Job>> = CopyOnWriteArrayList()


    fun taskUpdated(task: PlayTask) {
        this.task = task

        if (player == null && task.trigger == null) {
            createPlayer()
        }

        player?.gain = task.gain
        player?.isMute = task.mute
        player?.balance = task.balance
        if (!task.paused)
            player?.start()
        else player?.pause()
    }

    fun taskDeleted() {
        dispose()
    }

    /**
     * While this job is alive, its status can be queued.
     */
    fun status(): PlayTaskStatus {
        val task = this.task!!
        val gain = player?.gain ?: task.gain
        val mute = player?.isMute ?: task.mute
        val balance = player?.balance ?: task.balance
        val position = player?.position ?: task.position
        val duration = player?.duration ?: task.duration
        val paused = task.paused && player?.isPlaying == false
        val expandedTask = PlayTask(task.target, task.file, gain, mute, balance, position, duration, task.creator, paused, task.trigger, task)
        return PlayTaskStatus(expandedTask, player != null, finished, busyMessage, errorMessage, System.currentTimeMillis())
    }


    private fun createPlayer(): Player? {  // TODo error handling
        val file: MediaFile = DMediaFile(task!!.file)
        try {
            val player = engine.audioEngine.newPlayer(file)
            this.player = player
            player.prepare()
            player.activate(engine.speakerMap[task!!.target])
            player.addEndOfMediaListener { finished = true; notifyListeners() }
            if (player.getDuration() < 0) {
                Thread {
                    try {
                        player.waitForDurationProperty()
                        notifyListeners()
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

    fun isAlive(): Boolean {
        return task != null
    }

    fun dispose() {
        player?.dispose()
        player = null
        task = null
    }

    private fun notifyListeners() {
        for (listener in eventListeners) {
            listener.accept(this)
        }
    }
}