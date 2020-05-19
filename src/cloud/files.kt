package cloud

import java.io.*


/**
 * Cloud files can lie on any machine that is connected to the cloud.
 * Path, file size and type (directory or file) are available even after the origin device disconnects.
 * While the origin device is connected, [CloudFile.openStream] can be used to stream the file to the local device.
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
            cloud!!.openStream(origin, getPath())
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
        return "$path ($origin)"
    }


    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CloudFile

        if (origin != other.origin) {
            if (path == other.path) println("Two files have the same path '$path' but different origins: $origin, ${other.origin}")
            return false
        }
        if (path != other.path) return false

        return true
    }

    override fun hashCode(): Int {
        var result = origin.hashCode()
        result = 31 * result + path.hashCode()
        return result
    }
}