package distributed

import java.io.Serializable
import java.util.*

/**
 * A virtual distributed network ([DistributedPlatform]) consists of equals `Peer`s.
 *
 * Each peer has a public name and address which can be obtained using
 * [.getName] and [.getAddress], respectively. These are not
 * used to uniquely identify the peer but rather to serve as information to
 * display to the user.
 *
 * The isLocal machine is also represented as a Peer and can be accessed as Peer.SELF
 *
 * @author Philipp Holl
 */
class Peer(val isLocal: Boolean, val name: String, val address: String, val id: String) : Serializable {

    companion object {
        @JvmStatic
        fun getLocal(): Peer {
            return SELF
        }
        private val SELF = Peer(true, "isLocal", "localhost", UUID.randomUUID().toString())
    }

}
