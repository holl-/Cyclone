package cloud

import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable
import java.net.InetSocketAddress
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
class Peer(var isLocal: Boolean, var name: String, var address: String, var id: String) : Serializable
{
    companion object {
        @JvmStatic
        fun getLocal(): Peer {
            return SELF
        }
        private val SELF = Peer(true, getComputerName(), "localhost", UUID.randomUUID().toString())
    }


    internal var socketAddress: InetSocketAddress? = null


    private fun writeObject(stream: ObjectOutputStream) {
        stream.writeBoolean(isLocal)
        if (!isLocal) {
            stream.writeUTF(name)
            stream.writeUTF(address)
            stream.writeUTF(id)
        }
    }

    private fun readObject(stream: ObjectInputStream) {
        val thread = Thread.currentThread() as DeserializerThread
        val isIdentity = stream.readBoolean()
        if (isIdentity) {
            name = thread.fromPeer.name
            id = thread.fromPeer.id
            isLocal = thread.fromPeer.isLocal
            address = thread.fromPeer.address
        } else {
            name = stream.readUTF()
            address = stream.readUTF()
            id = stream.readUTF()
            isLocal = id == getLocal().id
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Peer) return false

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun toString(): String {
        return "$name ($id) @ $address"
    }

}

private fun getComputerName(): String {
    val env = System.getenv()
    return if (env.containsKey("COMPUTERNAME")) env["COMPUTERNAME"]!! else if (env.containsKey("HOSTNAME")) env["HOSTNAME"]!! else "Unknown Computer"
}