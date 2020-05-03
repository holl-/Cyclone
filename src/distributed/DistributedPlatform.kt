package distributed

import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.Serializable
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors
import java.util.function.BiConsumer
import java.util.function.Consumer
import java.util.function.Supplier

import sun.reflect.generics.reflectiveObjects.NotImplementedException
import distributed.internal.LocalFile

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
    private val mountedFiles = HashSet<RemoteFile>()

    private val connectionListeners = CopyOnWriteArrayList<ConnectionListener>()
    private val dataListeners = CopyOnWriteArrayList<DataListener>()
    var onMessageReceived: Consumer<Serializable>? = null

    private val localData = HashMap<String, Distributed>()

    private val eventHandler = Executors.newSingleThreadExecutor()

    // TODO stub implementation
    val allData: Collection<Distributed>
        get() = localData.values

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
     * Allows other peers to read the given file. The file will be available
     * using getMountedFiles or getFile where
     * the path equals the returned filename. If the file is a directory, all
     * contained files and folders are also shared.
     *
     *
     * *Warning:* While mounted, files should not be modified.
     *
     *
     * @return the mounted filename with which other peers can access the file.
     * This may be different from the real file name if a file with that
     * name has already been mounted before.
     * @param file
     * file to share
     * @see .unmountFile
     */
    fun mountFile(file: RemoteFile) {
        mountedFiles.add(file)
    }

    @Throws(IllegalArgumentException::class)
    fun mountVirtual(name: String, length: Long, lastModified: Long, streamSupplier: Supplier<InputStream>): RemoteFile? {
        return null
    }

    fun unmountFile(name: String) {

    }

    /**
     * Returns a list of all files mounted directly by the peer. Files can be
     * mounted using one of [DistributedPlatform]'s mount methods.
     *
     * @return a list of all files mounted by the peer
     * @throws IOException
     * if the peer is no longer available
     * @see .getFile
     */
    @Throws(IOException::class)
    fun getMountedFiles(peer: Peer): Collection<RemoteFile> {
        return if (peer.isLocal) {
            mountedFiles
        } else {
            throw NotImplementedException()
        }
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


    /**
     * Returns the file mounted at the given relative path. For more on
     * mounting, see [RemoteFile].
     *
     * @param path
     * mounted file path
     * @return the file mounted at the given relative path
     * @throws IOException
     * if the peer is no longer available
     */
    @Throws(IOException::class)
    fun getFile(path: String): RemoteFile {
        var rootName = path
        if (rootName.contains("/")) {
            rootName = rootName.substring(0, path.indexOf("/"))
        }
        if (rootName.contains("\\")) {
            rootName = rootName.substring(0, path.indexOf("\\"))
        }
        var root: LocalFile? = null
        for (file in mountedFiles) {
            if (file.path == rootName) root = file as LocalFile
        }
        if (root == null) throw IllegalArgumentException("Not found: $path")
        return if (path == rootName) root else LocalFile.createChild(root, path)
    }


    fun putData(data: Distributed) {
        if (data.platform != null)
            throw IllegalArgumentException("data is already bound")
        data.platform = this

        localData[data.id] = data

        val time = System.currentTimeMillis()
        val e = DataEvent(data, Peer.getLocal(), Peer.getLocal(), time, time)
        dataListeners.forEach { l -> l.onDataAdded(e) }
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

    fun getData(id: String): Optional<Distributed> {
        // TODO stub implementation
        return Optional.ofNullable(localData[id])
    }

    fun <T : Distributed> getOrAddData(addIfNotPresent: T): T {
        val p = getData(addIfNotPresent.id)
        if (p.isPresent) {
            return p.get() as T
        } else {
            putData(addIfNotPresent)
            return addIfNotPresent
        }
    }

    fun saveAllData(saveFile: File) {

    }

    fun loadAllData(saveFile: File) {

    }

    fun getPeer(id: String): Peer {
        if (Peer.getLocal().id == id) return Peer.getLocal()

        throw NotImplementedException()
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
