package audio

import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.function.Consumer
import java.util.logging.Logger

internal class SystemTimeObserver : Runnable {
    private val scheduler = Executors.newScheduledThreadPool(1)
    private var scheduleHandler: ScheduledFuture<*>? = null
    private val timeJumpListeners: MutableList<Consumer<Long>> = CopyOnWriteArrayList()
    private var lastTime: Long = -1
    private var updatePeriod = 0
    private val minJump = 2000

    fun start(durationMillis: Int) {
        if (scheduleHandler != null) {
            throw IllegalStateException("Already running")
        }
        updatePeriod = durationMillis
        scheduleHandler = scheduler.scheduleAtFixedRate(this, durationMillis.toLong(), durationMillis.toLong(), TimeUnit.MILLISECONDS)
    }

    override fun run() {
        val time = System.currentTimeMillis()
        if (lastTime > 0) {
            val dif = time - lastTime
            if (dif > updatePeriod + minJump) {
                for (l in timeJumpListeners) {
                    l.accept(dif)
                }
            }
        }
        lastTime = time
    }

    fun dispose() {
        scheduleHandler!!.cancel(false)
    }

    fun addSystemTimeJumpListener(l: Consumer<Long>) {
        timeJumpListeners.add(l)
    }

    fun removeSystemTimeJumpListener(l: Consumer<Long>) {
        timeJumpListeners.remove(l)
    }
}


fun createPauseOnStandby(engine: AudioEngine, logger: Logger?) {
    val stm = SystemTimeObserver()
    stm.addSystemTimeJumpListener(Consumer { timeDifference ->
        logger?.info("System time jumped by $timeDifference ms. Pausing all Players")
        for (player in engine.players) {
            player.paused.value = true
        } })
    stm.start(50)
}

/** Infers the media type from the file name ending. If the type is not understood, returns null. */
fun inferKnownMediaType(engine: AudioEngine, filename: String): MediaType? {
    if (!filename.contains(".")) return null
    val extension = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase()
    for (type in engine.supportedMediaTypes) {
        if (type.fileExtension == extension) return type
    }
    return null
}