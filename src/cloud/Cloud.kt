package cloud

import javafx.application.Platform
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.beans.value.ObservableValue
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.lang.IllegalStateException
import java.util.concurrent.CopyOnWriteArrayList
import java.util.function.Supplier
import java.util.logging.*
import java.util.stream.Stream

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
class Cloud {
    internal val allData = HashMap<Peer, ArrayList<Data>>()
    private val dataListeners = HashMap<Class<out Data>, Runnable>()
    private val viewedLists = HashMap<Class<out Data>, ObservableList<out Data>>()

    internal val sData = HashMap<Class<out SynchronizedData>, SimpleObjectProperty<out SynchronizedData>>()
    private val ownerMap = HashMap<Data, Any>()

    val onUpdate = CopyOnWriteArrayList<Runnable>()

    internal var multicast: CloudMulticast? = null
    internal var tcp: CloudTCP? = null
    internal var localPeer = Peer.getLocal()
    var logger = Logger.getLogger("cloud ${localPeer.id}")


    val peers = FXCollections.observableArrayList(Peer.getLocal())  // also contains disconnected peers
    val connectionStatus = SimpleStringProperty(null)


    init {
        allData[localPeer] = ArrayList()
    }


    internal fun initLocalPeer(peer: Peer) {
        localPeer = peer
        logger = Logger.getLogger("cloud ${peer.id}")
        peers.setAll(listOf(peer))
        allData.clear()
        allData[peer] = ArrayList()
    }


    /**
     * Connects to a virtual IP address and returns immediately. If other peers
     * are found at that address or later connect to it, they are added to the
     * lists of peers, returned by [.getAllPeers],
     * [.getRemotePeers] and the `onPeerConnected`-listener
     * is informed.
     *
     * @param multicastAddress a multicast address (in the range 224.0.0.0 to 239.255.255.255 for IPv4)
     * @param broadcastInterval delay between broadcast messages sent to multicast in milliseconds
     * @param autoConnect whether to immediately connect to other peers when discovered
     * @throws IOException
     * if the address cannot be connected to
     * @see .disconnectFromMulticastAddress
     * @see .addConnectionListener
     */
    @Throws(IOException::class)
    fun connect(multicastAddress: String = "225.139.25.1", multicastPort: Int = 5324, autoConnect: Boolean = true, broadcastInterval: Long = 1000) {
        disconnect()
        logger.level = Level.FINEST
        val tcp = CloudTCP(this, logger)
        tcp.startAccepting()
        val multicast = CloudMulticast(this, multicastAddress, multicastPort, tcp.serverSocket.localPort, tcp, logger, autoConnect, broadcastInterval)
        multicast.startPinging()
        multicast.startReceiving()
        this.tcp = tcp
        this.multicast = multicast
        Platform.runLater { connectionStatus.value = "UDP: ${multicastAddress}:${multicastPort}, TCP: ${tcp.serverSocket.localPort}" }
    }

    @Throws(IOException::class)
    fun disconnect() {
        multicast?.disconnect()
        tcp?.disconnect()
        multicast = null
        tcp = null
        Platform.runLater { connectionStatus.value = null }
    }

    fun isConnected(peer: Peer): Boolean {
        val connection = tcp?.connections?.firstOrNull { conn -> conn.peer == peer } ?: return false
        return connection.socket.isConnected
    }

    fun getLocalData(): ArrayList<Data> {
        if (localPeer in allData) {
            return allData[localPeer]!!
        } else {
            val exc = IllegalStateException()
            exc.printStackTrace()
            throw exc
        }
    }


    @Throws(IOException::class)
    internal fun openStream(peer: Peer, path: String, fileSize: Long): InputStream {
        val conn = tcp?.connections?.firstOrNull { c -> c.peer == peer } ?: throw IOException("Not connected")
        return conn.openFileStream(path, fileSize)
    }



    fun<T : SynchronizedData> getSynchronized(cls: Class<T>, default: Supplier<T>): ObservableValue<T> {
        @Suppress("UNCHECKED_CAST")
        if(cls in sData) return sData[cls] as ObservableValue<T>
        else {
            val defaultValue = default.get()
            val property = SimpleObjectProperty(defaultValue)
            sData[cls] = property
            pushSynchronized(defaultValue)
            return property
        }
    }

    /**
     * Updates a synchronized data object.
     * This may create a conflict.
     */
    fun pushSynchronized(data: SynchronizedData) {
        pushSynchronizedImpl(data, true)
    }

    fun pushSynchronizedImpl(data: SynchronizedData, localChange: Boolean) {
        // TODO this should upate immediately, may cause too many events otherwise
        Platform.runLater {
            if (data.javaClass in sData)
                sData[data.javaClass]?.value = data
            else
                sData[data.javaClass] = SimpleObjectProperty(data)
            fireUpdate()
            if (localChange) tcp?.synchronizedUpdated(data)
        }
    }

    fun remoteUpdateSynchronized(data: SynchronizedData, forceReplace: Boolean, isDataOlder: Boolean, logger: Logger?) {
        if (data.javaClass in sData && !forceReplace) {
            val localVersion = sData[data.javaClass]!!.value
            val resolved = if (isDataOlder) data.resolveConflict(localVersion) else localVersion.resolveConflict(data)
            logger?.fine("conflict: local = $localVersion, remote = $data -> $resolved")
            if (resolved.javaClass != data.javaClass) throw IllegalStateException("resolveConflict must return object of the same class")
            Platform.runLater {
                sData[data.javaClass]?.value = resolved
                fireUpdate()
            }
        } else {
            pushSynchronizedImpl(data, false)
        }
    }

    fun getAllCurrentSynchronized(): List<SynchronizedData> {
        val result = ArrayList<SynchronizedData>()
        for ((cls, prop) in sData) {
            result.add(prop.value)
        }
        return result
    }


    /**
     * Get all data objects (local and remote) that extend the class or interface.
     * As objects are added to or removed from the cloud, the returned list is updated.
     * All updates to the list are made from within the JavaFX thread.
     *
     * @return read-only list
     */
    fun<T : Data> getAll(cls: Class<T>): ObservableList<T> {
        @Suppress("UNCHECKED_CAST")
        if (cls in viewedLists)
            return viewedLists[cls] as ObservableList<T>

        val result = FXCollections.observableArrayList<T>()

        val listBuilder = Runnable {
            @Suppress("UNCHECKED_CAST")
            val filteredList = assembleAllData(cls) as List<T>
            Platform.runLater { result.setAll(filteredList) }
        }

        listBuilder.run()
        dataListeners[cls] = listBuilder
        viewedLists[cls] = result
        return result
    }

    private fun assembleAllData(cls: Class<*>): List<Data> {
        return allData.flatMap { (_, peerData) -> peerData.filter { d -> cls.isAssignableFrom(d.javaClass) } }
    }

    /**
     * Uploads a number of data objects to the cloud.
     * Objects equalling previously pushed objects of the same owner replace the old versions.
     * If [yankOthers] is true, previously pushed objects of the same class that are not part of [dataObjects] are yanked.
     */
    internal fun<T : Data> push(cls: Class<T>, dataObjects: Iterable<T>, owner:Any, yankOthers: Boolean) {
        val localData = allData[localPeer]!!
        val offlineList: MutableList<Data>
        offlineList = if(yankOthers) {
            ArrayList(localData.filter { d -> ownerMap[d] != owner || !cls.isAssignableFrom(d.javaClass) })
        } else {
            ArrayList(localData)
        }
        for(d in dataObjects) {
            ownerMap[d] = owner
            if(d in offlineList) {
                val index = offlineList.indexOf(d)
                offlineList[index] = d
            } else {
                offlineList.add(d)
            }
        }
        allData[localPeer] = offlineList
        notifyDataListeners(listOf(cls))
        tcp?.dataUpdated(offlineList, listOf(cls))
        fireUpdate()
    }

    fun yankAll(cls: Class<out Data>?, owner: Any) {
        val localData = allData[localPeer]!!
        val dataToRemove = localData.filter { d -> matches(d, cls, owner) }
        localData.removeAll(dataToRemove)
        val affectedClasses =
        if(cls != null) listOf(cls) else dataToRemove.map { d -> d.javaClass }.toSet()
        notifyDataListeners(affectedClasses)
        tcp?.dataUpdated(allData[localPeer]!!, affectedClasses)
        fireUpdate()
    }

    internal fun remoteUpdate(peer: Peer, affectedClasses: Collection<*>, data: List<*>) {
        val list = ArrayList<Data>()
        for (obj in data) {
            list.add(obj as Data)
        }
        allData[peer] = list
        @Suppress("UNCHECKED_CAST")
        notifyDataListeners(affectedClasses as Collection<Class<out Data>>)
        fireUpdate()
    }


    private fun matches(d: Data, cls: Class<out Data>?, owner: Any?): Boolean {
        val matchesClass = cls == null || cls.isAssignableFrom(d.javaClass)
        val matchesOwner = owner == null || ownerMap[d] == owner
        return matchesClass && matchesOwner
    }

    private fun notifyDataListeners(classes: Iterable<Class<out Data>>) {
        for ((listenerCls, listener) in dataListeners) {
            if(classes.any { cls ->  listenerCls.isAssignableFrom(cls)}) {
                listener.run()
            }
        }
    }


    fun write(data: List<Data>, file: File) {

    }

    fun read(file: File) {

    }

    fun peerDisconnected(peer: Peer) {
        allData.remove(peer)
        Platform.runLater { fireUpdate() }
    }

    internal fun fireUpdate() {
        onUpdate.forEach { r -> r.run() }
    }

}
