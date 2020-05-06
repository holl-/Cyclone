package player.model.data

import cloud.CloudFile
import cloud.Data
import java.util.*


/**
 * @param creator algorithm name that created the task
 */
open class Task(val creator: String, val paused: Boolean, val onFinished: List<Task>, baseTask: Task?) : Data() {
    val id: String = baseTask?.id ?: UUID.randomUUID().toString()


    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Task) return false

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}


/**
 *
 * @param position position within file in seconds
 * @param duration duration to play in seconds
 *
 * @param onFinished tasks that are executed once this task is finished.
 * These tasks must have the same target as this task.
 */
class PlayTask(
        val target: Speaker,
        val file: CloudFile,
        val gain: Double,
        val mute: Boolean,
        val balance: Double,
        val position: Double,
        val duration: Double?,
        creator: String,
        paused: Boolean,
        onFinished: List<Task>,
        baseTask: Task?
) : Task(creator, paused, onFinished, baseTask)
{
    override fun toString(): String {
        return "Play ${file.getName()} on $target, paused=$paused, gain=$gain, position=$position, duration=$duration, creator=$creator"
    }
}



//class WaitTask(val time: Long, onFinished: List<Task>) : Task(onFinished)


//data class PlayEvent(val relativeTime: Double, val absoluteOffset: Long) : Serializable


/**
 * This shared data object reflects the current status of playback. It is only
 * manipulated by the playback engine and contains properties that may be
 * displayed by the application.
 *
 * @param currentTasks startPosition in PlayTasks points to last known position, matching time
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
}

