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
    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false
        val speaker = o as Speaker
        return peer == speaker.peer && speakerId == speaker.speakerId
    }

    override fun hashCode(): Int {
        return Objects.hash(peer, speakerId)
    }

}