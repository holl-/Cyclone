package player.model

import cloud.Cloud
import cloud.CloudFile
import javafx.beans.InvalidationListener
import javafx.beans.binding.Bindings
import javafx.beans.property.SimpleObjectProperty
import player.model.data.PlayTask
import player.model.data.PlayTaskStatus
import player.model.data.Speaker
import player.model.data.TaskTrigger
import java.lang.IllegalStateException
import java.util.*
import java.util.concurrent.Callable
import java.util.function.Function
import kotlin.collections.ArrayList


/**
 * @param maxTasks number of tasks that can be pushed to the cloud at the same time, must be >= 1
 */
class TaskChainBuilder(val cloud: Cloud, val fileChain: Function<CloudFile, CloudFile?>, val creator: String, val maxTasks: Int = 3)
{
    private val statuses = cloud.getAll(PlayTaskStatus::class.java)
    private val tasks = ArrayList<PlayTask>()
    private var active = false
    private var speaker: Speaker? = null
    val currentTask = SimpleObjectProperty<PlayTask?>()
    val currentStatus = Bindings.createObjectBinding(Callable { currentTask.value?.let { task -> status(task) } }, currentTask, statuses)

    var paused: Boolean = false
        set(value) {
            if (field == value) return
            field = value
            if (active) update()
        }
    // balance, mute, gain can be implemented like paused


    init {
        statuses.addListener(InvalidationListener {
            if (active && !tasks.isEmpty() && status(tasks[0])?.finished == true) {
                update()  // A song just finished
            }
        })
    }

    fun deactivate() {
        if (!active) return
        active = false
        speaker = null
        cloud.yankAll(PlayTask::class.java, this)
        tasks.clear()
    }

    /**
     * Must be called before [play].
     */
    fun activate(speaker: Speaker) {
        if (active && speaker == this.speaker) return
        active = true
        this.speaker = speaker
        update()
    }

    fun isPlaying(): Boolean {
        return tasks.isNotEmpty()
    }

    /**
     * Starts playing the chain from the given position.
     */
    fun play(file: CloudFile, position: Double) {
        val speaker = this.speaker ?: throw IllegalStateException("No speaker set")
        when {
            tasks.isEmpty() -> {
                tasks.add(PlayTask(speaker, file, 0.0, false, 0.0, position, 0, null, creator, paused, null, newId()))
            }
            tasks.firstOrNull()?.file == file -> {  // is this the active task? -> keep it
                // jump within file
                tasks[0] = PlayTask(speaker, file, 0.0, false, 0.0, position, tasks[0].restartCount + 1, null, creator, paused, null, tasks[0].id)
            }
            else -> {  // is this a scheduled task? Move forward, remove trigger, remove previous
                for (task in ArrayList(tasks)) {
                    if (task.file == file) {
                        tasks[0] = PlayTask(speaker, file, 0.0, false, 0.0, position, task.restartCount, null, creator, paused, null, task.id)
                        break
                    } else {
                        tasks.removeAt(0)
                    }
                }
                if (tasks.isEmpty()) {
                    tasks.add(PlayTask(speaker, file, 0.0, false, 0.0, position, 0, null, creator, paused, null, newId()))
                }
            }
        }
        update()
    }


    private fun update() {
        if (tasks.isEmpty()) return  // play() must be called
        if (speaker == null) return // activate() must be called

        // Can we remove finished tasks?
        var lastFinished: PlayTask? = null
        while (tasks.firstOrNull()?.let { task -> status(task)?.finished } == true ){
            lastFinished = tasks.removeAt(0)
        }

        // Go through the list of tasks. Do tasks have to be altered? Do they still match the playlist or do we have to discard part of the chain?
        for ((index, task) in tasks.withIndex()) {
            // Adjust this task's properties, file should be correct
            val adjustedTask = PlayTask(speaker!!, task.file, 0.0, false, 0.0, task.position, task.restartCount, null, creator, paused, task.trigger, task.id)
            tasks[index] = adjustedTask

            // Check whether next task should be discarded
            if (index < tasks.size - 1) {
                val nextFile = fileChain.apply(task.file)
                val nextTask = tasks[index + 1]
                if (nextTask.file != nextFile) {
                    tasks.removeAll(tasks.subList(index + 1, tasks.size))
                    break
                }
            }
        }

        // If there is no task left (should only happen if maxTasks=1), build a new one
        if (tasks.size == 0) {
            fileChain.apply(lastFinished!!.file)?.let { file ->
                tasks.add(PlayTask(speaker!!, file, 0.0, false, 0.0, 0.0, 0, null, creator, paused, null, newId()))
            }
        }

        if (tasks.isNotEmpty()) {
            // Add new tasks if we have too few
            while (tasks.size < maxTasks) {
                fileChain.apply(tasks.last().file)?.let { file ->
                    tasks.add(PlayTask(speaker!!, file, 0.0, false, 0.0, 0.0, 0, null, creator, paused, TaskTrigger(tasks.last().id), newId()))
                } ?: break
            }
        }

        currentTask.value = tasks.firstOrNull()
        cloud.push(PlayTask::class.java, tasks, this, true)
    }


    private fun status(task: PlayTask): PlayTaskStatus? {
        return statuses.firstOrNull { s -> s.task == task && s.task.target == task.target }
    }

    private fun newId(): String {
        return "$creator-${UUID.randomUUID()}"
    }
}