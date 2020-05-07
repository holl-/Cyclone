package cloud

import javafx.application.Platform
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.value.ObservableValue
import javafx.collections.FXCollections
import javafx.collections.ListChangeListener
import javafx.collections.ObservableList
import javafx.collections.transformation.FilteredList
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.Serializable
import java.lang.UnsupportedOperationException
import java.util.concurrent.Callable
import java.util.function.BiConsumer
import java.util.function.Consumer
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

    private var pushedData = ArrayList<Data>()
    private val dataListeners = HashMap<Class<out Data>, Consumer<List<Data>>>()
    private val viewedLists = HashMap<Class<out Data>, ObservableList<out Data>>()

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
            println("Cloud: Updating list for ${cls.simpleName}: $filteredList")
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
