package player.model.data

import cloud.CloudFile
import cloud.Data
import cloud.SynchronizedData
import java.io.Serializable


/**
 * The master gain affects all tasks.
 * It is added to the individual gain levels of the tasks.
 */
data class MasterGain(val value: Double) : SynchronizedData()


/**
 * When updating an existing task, the follogin properties cannot be changed: [file], [id]
 *
 *
 * Updating old task vs creating new
 * - Change speaker on same PC -> Player.setDevice()
 *
 * @param position position within file in seconds
 * @param duration duration to play in seconds
 * @param restartCount resets to []position] if this value increases.
 * This cannot be used to revive tasks that are already finished.
 * @param trigger References another task that must finish before this one is started.
 * Both tasks must have the same target as this task.
 */
class PlayTask(
        val target: Speaker,
        val file: CloudFile,
        val gain: Double,
        val mute: Boolean,
        val balance: Double,
        val position: Double,
        val restartCount: Int,  //
        val duration: Double?,
        val creator: String,
        val paused: Boolean,
        val trigger: TaskTrigger?,
        val id: String
) : Data()
{
    override fun toString(): String {
        return "Play '${file.getName()}' on $target, paused=$paused, gain=$gain, position=$position, duration=$duration, creator=$creator"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PlayTask) return false

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}


class TaskTrigger(val taskId: String) : Serializable



//class WaitTask(val time: Long, onFinished: List<Task>) : Task(onFinished)


//data class PlayEvent(val relativeTime: Double, val absoluteOffset: Long) : Serializable


/**
 * This shared data object reflects the current status of playback. It is only
 * manipulated by the playback engine and contains properties that may be
 * displayed by the application.
 *
 * @param task startPosition in task points to last known position, matching time
 * duration in PlayTasks points to total duration of task, non-null
 *
 * @param time time (in milliseconds) when the status was obtained
 *
 * @param active if True and task not paused -> playing. False if loading or error
 *
 * @author Philipp Holl
 */
class PlayTaskStatus(
        val task: PlayTask,
        val active: Boolean,
        val finished: Boolean,
        val busyMessage: String?,
        val errorMessage: String?,
        val time: Long
) : Data()
{
    fun message(): String? {
        if(errorMessage != null) return errorMessage
        if(busyMessage != null) return busyMessage
        return null
    }

    fun extrapolatePosition(): Double {
        return if (!active || task.paused) task.position else task.position + (System.currentTimeMillis() - time) / 1e3
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PlayTaskStatus) return false

        if (task != other.task) return false

        return true
    }

    override fun hashCode(): Int {
        return task.hashCode()
    }

    override fun toString(): String {
        val status = if(finished) "Finished" else if(active) "Active" else "Inactive"
        return if(message() != null) status + message() else status
    }
}

