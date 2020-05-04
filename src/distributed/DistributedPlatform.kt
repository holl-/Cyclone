package distributed

import java.io.File
import java.io.IOException
import java.io.Serializable
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors
import java.util.function.BiConsumer
import java.util.function.Consumer
import kotlin.collections.HashMap

/**
 * Main class for setting up a virtual distributed platform.
 *
 */
/**
 * Creates a new virtual distributed platform. The created platform will
 * only know the isLocal peer and does not actively connect to other peers or
 * receive connections from such until a `connect` method is
 * called.
 */
class DistributedPlatform {
    private val mountedFiles = HashSet<DFile>()

    private val connectionListeners = CopyOnWriteArrayList<ConnectionListener>()
    private val dataListeners = CopyOnWriteArrayList<DataListener>()
    var onMessageReceived: Consumer<Serializable>? = null

    private val rootObjects = HashMap<Class<out Distributed>, Distributed>()

    private val eventHandler = Executors.newSingleThreadExecutor()


    val remotePeers: List<Peer>?
        get() = null

    val allPeers: List<Peer>?
        get() = null

    /**
     * Connects to a virtual IP address and returns immediately. If other peers
     * are found at that address or later connect to it, they are added to the
     * lists of peers, returned by [.getAllPeers],
     * [.getRemotePeers] and the `onPeerConnected`-listener
     * is informed.
     *
     * @param multicastAddress
     * a multicast address (in the range 224.0.0.0 to 239.255.255.255
     * for IPv4)
     * @throws IOException
     * if the address cannot be connected to
     * @see .disconnectFromMulticastAddress
     * @see .addConnectionListener
     */
    @Throws(IOException::class)
    fun connectToMulticastAddress(multicastAddress: String) {

    }

    @Throws(IOException::class)
    fun connectToExternal(address: String) {

    }

    @Throws(IOException::class)
    fun disconnectFromMulticastAddress(multicastAddress: String) {

    }

    @Throws(IOException::class)
    fun disconnectFromExternal(address: String) {

    }

    fun disconnectAll(errorHandler: BiConsumer<String, IOException>) {

    }


    /**
     * Sends a message to the peer. Messages can be any serializable object. The
     * peer can listen for incoming messages using
     * [DistributedPlatform.setOnMessageReceived].
     *
     * @param message
     * message object to send
     * @throws IOException
     * if the peer is no longer available
     */
    @Throws(IOException::class)
    fun send(message: Serializable, target: Peer) {

    }


    fun putObject(data: Distributed) {
        if (data.platform != null)
            throw IllegalArgumentException("data is already bound")
        data.platform = this

        if(data.javaClass in rootObjects) {
            throw IllegalStateException("An instance of class ${data.javaClass} is already registered.")
        }

        rootObjects[data.javaClass] = data

        val time = System.currentTimeMillis()
        val e = DataEvent(data, Peer.getLocal(), Peer.getLocal(), time, time)
        dataListeners.forEach { l -> l.onDataAdded(e) }
    }

    fun putData(objects: List<Distributed>) {
        for(obj in objects) {
            putObject(obj)
        }
    }

    fun removeData(data: Distributed) {
        if (data.platform !== this)
            throw IllegalArgumentException()
        data.platform = null

        val time = System.currentTimeMillis()
        val e = DataEvent(data, Peer.getLocal(), Peer.getLocal(), time, time)
        dataListeners.forEach { l -> l.onDataRemoved(e) }
    }

    internal fun changed(data: Distributed) {
        eventHandler.execute {
            val time = System.currentTimeMillis()
            val e = DataEvent(data, Peer.getLocal(), Peer.getLocal(), time, time)
            dataListeners.forEach { l -> l.onDataAdded(e) }
            data._fireChanged(e)
        }
    }

    fun<T : Distributed> getData(cls: Class<T>): T? {
        return rootObjects[cls] as T
    }

    fun hasData(cls: Class<out Distributed>): Boolean {
        return cls in rootObjects
    }

    fun saveAllData(saveFile: File) {

    }

    fun loadAllData(saveFile: File) {

    }

    fun getPeer(id: String): Peer {
        if (Peer.getLocal().id == id) return Peer.getLocal()
        TODO()
    }

    fun addConnectionListener(l: ConnectionListener) {
        connectionListeners.add(l)
    }

    fun removeConnectionListener(l: ConnectionListener) {
        connectionListeners.remove(l)
    }

    fun addDataListener(l: DataListener) {
        dataListeners.add(l)
    }

    fun removeDataListener(l: DataListener) {
        dataListeners.remove(l)
    }

}
