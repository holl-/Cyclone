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
import java.util.concurrent.CopyOnWriteArrayList
import java.util.function.Consumer
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
    internal var pushedData = ArrayList<Data>()
    private val dataListeners = HashMap<Class<out Data>, Consumer<List<Data>>>()
    private val viewedLists = HashMap<Class<out Data>, ObservableList<out Data>>()

    internal val sData = HashMap<Class<out SynchronizedData>, SimpleObjectProperty<out SynchronizedData>>()
    private val ownerMap = HashMap<Data, Any>()

    val onUpdate = CopyOnWriteArrayList<Runnable>()

    private var multicast: CloudMulticast? = null
    private var tcp: CloudTCP? = null
    internal var peer = Peer.getLocal()
        set(value) {
            field = value
            logger = Logger.getLogger("cloud ${value.id}")
            peers.setAll(listOf(value))
        }
    var logger = Logger.getLogger("cloud ${peer.id}")


    val peers = FXCollections.observableArrayList(Peer.getLocal())
    val connectionStatus = SimpleStringProperty(null)


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
    fun connect(multicastAddress: String = "225.4.5.6", multicastPort: Int = 5324, localTCPPort: Int = 5325) {
        disconnect()
        logger.level = Level.FINEST
//        val cons = ConsoleHandler()
//        cons.level = Level.INFO
//        logger.addHandler(cons)
        tcp = CloudTCP(this, localTCPPort, logger)
        tcp!!.startAccepting()
        multicast = CloudMulticast(this, multicastAddress, multicastPort, localTCPPort, tcp!!, logger)
        multicast!!.startPinging()
        multicast!!.startReceiving()

        Platform.runLater { connectionStatus.value = "UDP: $multicastAddress:$multicastPort, TCP: $localTCPPort" }
    }

    @Throws(IOException::class)
    fun disconnect() {
        multicast?.disconnect()
    }

    fun isConnected(peer: Peer): Boolean {
        val connection = tcp?.connections?.firstOrNull { conn -> conn.peer == peer } ?: return false
        return connection.socket.isConnected
    }


    @Throws(IOException::class)
    internal fun listFiles(peer: Peer, path: String): Stream<CloudFile> {
        TODO("not required for first version")
    }

    @Throws(IOException::class)
    internal fun openStream(peer: Peer, path: String): InputStream {
        TODO()
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

    internal fun pushSynchronizedImpl(data: SynchronizedData, localChange: Boolean) {
        Platform.runLater(Runnable {
            if (data.javaClass in sData) sData[data.javaClass]?.value = data
            else {
                sData[data.javaClass] = SimpleObjectProperty(data)
            }
            onUpdate.forEach { r -> r.run() }
            if (localChange) tcp?.synchronizedUpdated(data)
        })
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

        val listBuilder = Consumer<List<Data>> { offlineList ->
            @Suppress("UNCHECKED_CAST")
            val filteredList: List<T> = offlineList.filter { d -> cls.isAssignableFrom(d.javaClass) } as List<T>
            result.setAll(filteredList)
        }

        listBuilder.accept(pushedData)
        dataListeners[cls] = listBuilder
        viewedLists[cls] = result
        return result
    }

    /**
     * Uploads a number of data objects to the cloud.
     * Objects equalling previously pushed objects of the same owner replace the old versions.
     * If [yankOthers] is true, previously pushed objects of the same class that are not part of [dataObjects] are yanked.
     */
    fun<T : Data> push(cls: Class<T>, dataObjects: Iterable<T>, owner:Any, yankOthers: Boolean) {
        val offlineList: MutableList<Data>
        offlineList = if(yankOthers) {
            ArrayList(pushedData.filter { d -> ownerMap[d] != owner || !cls.isAssignableFrom(d.javaClass) })
        } else {
            ArrayList(pushedData)
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
        pushedData = offlineList
        notifyDataListener(listOf(cls))
    }

    private fun yank(d: Data) {
        pushedData.remove(d)
        ownerMap.remove(d)
        notifyDataListener(listOf(d.javaClass))
    }

    fun yankAll(cls: Class<out Data>?, owner: Any) {
        val dataToRemove = pushedData.filter { d -> matches(d, cls, owner) }
        pushedData.removeAll(dataToRemove)
        if(cls != null) {
            notifyDataListener(listOf(cls))
        } else {
            val affectedClasses = dataToRemove.map { d -> d.javaClass }.toSet()
            notifyDataListener(affectedClasses)
        }
    }

    private fun matches(d: Data, cls: Class<out Data>?, owner: Any?): Boolean {
        val matchesClass = cls == null || cls.isAssignableFrom(d.javaClass)
        val matchesOwner = owner == null || ownerMap[d] == owner
        return matchesClass && matchesOwner
    }

    private fun notifyDataListener(classes: Iterable<Class<out Data>>) {
        val offlineList = ArrayList(pushedData)
        Platform.runLater {
            for ((listenerCls, listener) in dataListeners) {
                if(classes.any { cls ->  listenerCls.isAssignableFrom(cls)}) {
                    listener.accept(offlineList)
                }
            }
        }
    }


    fun write(data: List<Data>, file: File) {

    }

    fun read(file: File) {

    }

}
