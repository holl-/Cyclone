package player.model.data

import distributed.DFile
import distributed.Distributed
import java.util.*

/**
 * This shared data object contains all commands for the playback engine. This
 * includes what media to play, the volume, etc. Except for the target position,
 * all fields should indicate the values the playback engine should use.
 *
 * @author Philipp Holl
 */
class PlayerTarget : Distributed(true, false) {
    var programControllerId: String? = null

    var targetDevice: Optional<Speaker> = Optional.empty()
        set(targetDevice) {
            field = targetDevice
            fireChangedLocally()
        }

    /**
     * if empty, dispose of player
     */
    var targetMedia: Optional<DFile> = Optional.empty()
        private set
    var targetGain = 0.0
        set(targetGain) {
            field = targetGain
            fireChangedLocally()
        }
    var isTargetMute = false
        set(targetMute) {
            field = targetMute
            fireChangedLocally()
        }
    private var targetPlaying = false
    var targetPosition = OptionalDouble.empty()
        private set

    /** The time at which the target position request was issued  */
    var positionUpdateTime: Long = 0
        private set
    var isLoop = true
        set(loop) {
            field = loop
            fireChangedLocally()
        }
    var isShuffled = false
        set(shuffled) {
            field = shuffled
            fireChangedLocally()
        }

    fun setTargetMedia(targetMedia: DFile?, startPlayingImmediately: Boolean, controllerId: String) {
        setTargetMedia(Optional.of(targetMedia!!), startPlayingImmediately, controllerId)
    }

    fun setTargetMedia(targetMedia: Optional<DFile>, startPlayingImmediately: Boolean, controllerId: String) {
        this.targetMedia = targetMedia
        if (startPlayingImmediately) {
            targetPlaying = true
        }
        if (!targetMedia.isPresent) {
            targetPlaying = false
        }
        setTargetPosition(0.0, false)
        this.programControllerId = controllerId;
    }

    fun isTargetPlaying(): Boolean {
        return targetPlaying
    }

    fun setTargetPlaying(targetPlaying: Boolean) {
        this.targetPlaying = targetPlaying
        fireChangedLocally()
    }

    fun setTargetPosition(targetPosition: Double, startPlaying: Boolean) {
        if (targetPosition < 0) throw IllegalArgumentException("position < 0")
        this.targetPosition = OptionalDouble.of(targetPosition)
        positionUpdateTime = System.currentTimeMillis()
        if (startPlaying) {
            targetPlaying = true
        }
        fireChangedLocally()
    }

    fun wasTargetPositionSetAfter(lastUpdateTime: Long): Boolean {
        return positionUpdateTime > lastUpdateTime && targetPosition.isPresent
    }

    fun stop() {
        targetPlaying = false
        setTargetPosition(0.0, false)
    }

}