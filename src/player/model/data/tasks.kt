package player.model.data

import cloud.DFile
import cloud.Data
import cloud.SynchronizedData
import java.io.Serializable
import java.util.*


/**
 * Holds Tasks to be executed immediately or have been executed.
 */
data class Program(
        val tasks: List<Task>,
        val creator: String,
        val paused: Boolean
) : SynchronizedData()



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
 * @author Philipp Holl
 */
class PlaybackStatus(
        val currentTasks: List<Task>,
        val playing: Boolean,
        val busyMessage: String?,
        val errorMessage: String?,
        val time: Long
) : Data()


fun extrapolationPosition(task: PlayTask, time: Long, paused: Boolean): Double {
    return if (paused) task.startPosition else task.startPosition + (System.currentTimeMillis() - time) / 1e3
}



open class Task(val onFinished: List<Task>) : Serializable {
    val id = UUID.randomUUID().toString()
}



class PlayTask(
        val target: Speaker,
        val file: DFile,
        val gain: Double,
        val mute: Boolean,
        val balance: Double,
        val startPosition: Double,
        val duration: Double?,
//        val events: Map<PlayEvent, Task>,
        onFinished: List<Task>
) : Task(onFinished)


//data class PlayEvent(val relativeTime: Double, val absoluteOffset: Long) : Serializable


