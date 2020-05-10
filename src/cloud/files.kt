package cloud

import java.io.*
import java.util.*
import java.util.stream.Stream


/**
 * Cloud files can lie on any machine that is connected to the cloud.
 * While the origin machine is connected, [CloudFile.list] and [CloudFile.openStream] can be used to query file information from the host.
 */
class CloudFile(file: File) : Data() {
    private var path: String = file.path
    private var size: Long? = null  // read upon serialization
    private var isDir: Boolean? = null // read upon serialization

    @Transient private var origin: Peer = Peer.getLocal()
    @Transient private var cloud: Cloud? = null


    private fun writeObject(stream: ObjectOutputStream) {
        stream.writeUTF(path)
        stream.writeLong(length())
        stream.writeBoolean(isDirectory())
        stream.writeObject(origin)
    }

    private fun readObject(stream: ObjectInputStream) {
        path = stream.readUTF()
        size = stream.readLong()
        isDir = stream.readBoolean()
        origin = stream.readObject() as Peer
        val thread = Thread.currentThread() as DeserializerThread
        cloud = thread.cloud
    }


    fun getPath(): String {
        return path
    }

    fun getName() : String {
        val parts = path.replace('\\', '/').split("/")
        return parts[parts.size - 1]
    }

    fun isDirectory(): Boolean {
        if(isDir == null && originatesHere())
            isDir = File(path).isDirectory
        return isDir!!
    }

    /**
     * Returns the size of this file in bytes.
     *
     * Files should not be modified while they are being mounted by a
     * [Cloud]. Therefore this method also returns a valid size if the
     * hosting peer is not available.
     *
     * @return the size of this file in bytes
     * @throws UnsupportedOperationException
     * if this [CloudFile] represents a directory
     */
    @Throws(UnsupportedOperationException::class)
    fun length(): Long {
        if(isDirectory()) throw java.lang.UnsupportedOperationException("length() unavailable for directories. " + getPath())
        if(size == null && originatesHere()) {
            size = File(path).length()
        }
        return size!!
    }

    /**
     * Obtains a list of files contained in this directory by the remote peer.
     *
     * @return files contained in this directory
     * @throws UnsupportedOperationException
     * if this file is not a directory
     * @throws IOException
     * if the remote peer is not available
     */
    @Throws(UnsupportedOperationException::class, IOException::class)
    fun list(): Stream<CloudFile> {
        if(!isDirectory()) throw java.lang.UnsupportedOperationException("list() unavailable for files. " + getPath())
        return if(originatesHere()) {
            val dir = File(path)
            val names = dir.list()
            Arrays.stream(names).map { name -> CloudFile(File(dir, name)) }
        } else {
            cloud!!.listFiles(origin, path)
        }
    }

    /**
     * Opens an `InputStream` for this file.
     *
     * @return an `InputStream` for this file
     * @throws IOException
     * if the hosting peer is not available or the connection is
     * interrupted
     * @throws UnsupportedOperationException
     * if this is a directory
     */
    @Throws(IOException::class, UnsupportedOperationException::class)
    fun openStream(): InputStream {
        return if(originatesHere()) {
            FileInputStream(File(path))
        } else {
            cloud!!.openStream(origin, getPath(), size!!)
        }
    }

    /**
     * A file originates here if its associated peer is the local peer.
     *
     * @return if this file is stored locally
     * @see Peer.isLocal
     */
    fun originatesHere(): Boolean {
        return origin.isLocal
    }

    fun getOrigin(): Peer {
        return origin
    }


    override fun toString(): String {
        return path
    }


    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CloudFile

        if (origin != other.origin) return false
        if (path != other.path) return false

        return true
    }

    override fun hashCode(): Int {
        var result = origin.hashCode()
        result = 31 * result + path.hashCode()
        return result
    }
}