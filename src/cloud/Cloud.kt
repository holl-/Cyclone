package cloud

import javafx.beans.binding.Bindings
import javafx.beans.value.ObservableValue
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.collections.transformation.FilteredList
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.Serializable
import java.util.concurrent.Callable
import java.util.function.BiConsumer
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

    private val data = FXCollections.observableArrayList<Data>()
    private val sData = FXCollections.observableArrayList<SynchronizedData>()

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


    fun<T : Data> getData(cls: Class<T>): ObservableList<T> {
        @Suppress("UNCHECKED_CAST")
        return FXCollections.unmodifiableObservableList(FilteredList<Data>(data) {d -> d.javaClass == cls} as ObservableList<T>)
    }

    fun<T : SynchronizedData> getSynchronizedData(cls: Class<T>): ObservableValue<T?> {
        @Suppress("UNCHECKED_CAST") val generator: Callable<T?> = Callable { data.firstOrNull { d -> d.javaClass == cls} as T? }
        return Bindings.createObjectBinding(generator, data)
    }


    /**
     * If data is not shared, adds it to the owner-bound accessible resources.
     *
     * If data is shared, tries to replace an existing shared copy with this one.
     */
    fun putData(d: Data) {
        if (d.platform != null)
            throw IllegalArgumentException("data is already bound")
        d.platform = this

        if(d is SynchronizedData) {
            if(sData.any { d -> d.javaClass == d.javaClass }) {
                val index = sData.indexOfFirst { d -> d.javaClass == d.javaClass }
                sData[index] = d
            } else {
                sData.add(d)
            }
        }
        else {
            data.add(d)
        }
    }

//    fun removeData(data: Data) {
//        if (data.platform !== this)
//            throw IllegalArgumentException()
//        data.platform = null
//
//        val time = System.currentTimeMillis()
//        val e = DataEvent(data, Peer.getLocal(), Peer.getLocal(), time, time)
//        dataListeners.forEach { l -> l.onDataRemoved(e) }
//    }


    fun saveSynchronizedData(saveFile: File) {

    }

    fun loadSynchronizedData(saveFile: File) {

    }

}
