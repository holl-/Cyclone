package cloud

import java.io.Serializable
import java.util.*

/**
 * A virtual distributed network ([Cloud]) consists of equals `Peer`s.
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
class Peer(val isLocal: Boolean, val name: String, val address: String, val id: String) : Serializable
{
    companion object {
        @JvmStatic
        fun getLocal(): Peer {
            return SELF
        }
        private val SELF = Peer(true, getComputerName(), "localhost", UUID.randomUUID().toString())
    }

    // address is filled in upon deserialization
}

private fun getComputerName(): String {
    val env = System.getenv()
    return if (env.containsKey("COMPUTERNAME")) env["COMPUTERNAME"]!! else if (env.containsKey("HOSTNAME")) env["HOSTNAME"]!! else "Unknown Computer"
}