package player.model.data

import distributed.Distributed
import distributed.Peer
import java.util.*

class Speaker(val peer: Peer,
              val speakerId: String,
              val name: String,
              val minGain: Double,
              val maxGain: Double,
              val isPeerDefault: Boolean) : Distributed(false, true) {

    override fun toString(): String {
        return name
    }

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