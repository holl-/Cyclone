package player.model.data

import distributed.Distributed
import java.util.*

class SpeakerList : Distributed(false, false) {

    var speakers: List<Speaker> = Collections.emptyList()
        set(value) {
            field = Collections.unmodifiableList(value)
            fireChangedLocally()
        }

}