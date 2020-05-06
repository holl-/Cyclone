package cloud

import javafx.application.Platform
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.value.ObservableValue
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.collections.transformation.FilteredList
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.Serializable
import java.lang.UnsupportedOperationException
import java.util.function.BiConsumer
import java.util.function.Predicate
import java.util.function.Supplier
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

    val peers: ObservableList<Peer> = FXCollections.observableArrayList()
        get() = FXCollections.unmodifiableObservableList(field)

    private val pushedData = FXCollections.observableArrayList<Data>()
    private val sData = HashMap<Class<out SynchronizedData>, SimpleObjectProperty<out SynchronizedData>>()
//    private val sData = FXCollections.observableArrayList<SynchronizedData>()
    private val ownerMap = HashMap<Data, Any>()

//    private val eventHandler = Executors.newSingleThreadExecutor()


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


    @Throws(IOException::class)
    internal fun<T : Serializable> query(query: Serializable, target: Peer): Stream<T> {
        TODO()
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
            return property
        }
    }

    /**
     * Updates a synchronized data object.
     * This may create a conflict.
     */
    fun pushSynchronized(d: SynchronizedData) {
        Platform.runLater(Runnable { (getSynchronized(d.javaClass, Supplier { d }) as SimpleObjectProperty).value = d })
    }





    fun<T : Data> getAll(cls: Class<T>): ObservableList<T> {
        // TODO this is throwing events even when no instance of cls changes
        @Suppress("UNCHECKED_CAST")
        return FilteredList<Data>(pushedData, Predicate { d -> cls.isAssignableFrom(d.javaClass) }) as ObservableList<T>
    }

    /**
     * Uploads a number of data objects to the cloud.
     * Objects equalling previously pushed objects of the same owner replace the old versions.
     * If [yankOthers] is true, previously pushed objects of the same class that are not part of [dataObjects] are yanked.
     */
    fun<T : Data> push(cls: Class<T>, dataObjects: Iterable<T>, owner:Any, yankOthers: Boolean) {
        Platform.runLater(Runnable {
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
            println(offlineList)
            pushedData.setAll(offlineList)
        })
    }

    private fun yank(d: Data) {
        if (d.cloud !== this)
            throw IllegalArgumentException()
        d.cloud = null

        if(d is SynchronizedData) {
            throw UnsupportedOperationException("SynchronizedData instances cannot be yanked.")
        } else {
            pushedData.remove(d)
        }

        ownerMap.remove(d)
    }

    fun yankAll(cls: Class<out Data>?, owner: Any?) {
        for (d in ArrayList(pushedData)) {  // copy the list to avoid threading issues
            val matchesClass = cls == null || cls.isAssignableFrom(d.javaClass)
            val matchesOwner = owner == null || ownerMap[d] == owner
            if (matchesClass && matchesOwner) {
                yank(d)
            }
        }
    }


    fun write(data: List<Data>, file: File) {

    }

    fun read(file: File) {

    }

}
