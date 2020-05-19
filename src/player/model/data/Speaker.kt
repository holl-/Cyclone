package player.model.data

import cloud.Data
import cloud.Peer
import java.util.*

/**
 * @param isDefault whether this is the default speaker of [peer]
 */
data class Speaker(val peer: Peer,
              val speakerId: String,
              val name: String,
              val minGain: Double,
              val maxGain: Double,
              val isDefault: Boolean) : Data()
{
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val speaker = other as Speaker
        return peer == speaker.peer && speakerId == speaker.speakerId
    }

    override fun hashCode(): Int {
        return Objects.hash(peer, speakerId)
    }

}