package distributed

import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.util.*
import java.util.stream.Stream

class DFile(file: File) : Distributed(false, true) {
    /** The peer that hosts this file */
    private val originPeer: Peer = Peer.getLocal()
    private val path: String = file.path
    private var size: Long? = null  // read upon serialization
    private var isDir: Boolean? = null // read upon serialization


    /**
     * Gets the path to this file under which it is mounted.
     *
     *
     * Example: The folder C:/music which contains the file song.mp3 was mounted
     * using [DistributedPlatform.mountFile]. Then the path to song.mp3 will be
     * music/song.mp3.
     *
     *
     * @return the path to this file under which it is mounted
     */
    fun getPath(): String {
        return path
    }

    fun getName() : String {
        val parts = path.split("/|\\\\")
        return parts[parts.size - 1]
    }

    /**
     * Returns true if this object represents a directory. This method may not
     * check if the path is still available. If not, the result of this method
     * is undefined.
     *
     * @return true if this object represents a directory
     */
    fun isDirectory(): Boolean {
        if(isDir == null && originatesHere())
            isDir = File(path).isDirectory
        return isDir!!
    }

    /**
     * Returns the size of this file in bytes.
     *
     *
     * Files should not be modified while they are being mounted by a
     * [DistributedPlatform]. Therefore this method also returns a valid size if the
     * hosting peer is not available.
     *
     *
     * @return the size of this file in bytes
     * @throws UnsupportedOperationException
     * if this [DFile] represents a directory
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
    fun list(): Stream<DFile> {
        if(!isDirectory()) throw java.lang.UnsupportedOperationException("list() unavailable for files. " + getPath())
        if(originatesHere()) {
            val dir = File(path)
            val names = dir.list()
            return Arrays.stream(names).map { name -> DFile(File(dir, name)) }
        } else {
            TODO("Query from host")
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
        if(originatesHere()) {
            return FileInputStream(File(path))
        } else {
            TODO()
        }
    }

    /**
     * A file originates here if its associated peer is the local peer.
     *
     * @return if this file is stored locally
     * @see Peer.isLocal
     */
    fun originatesHere(): Boolean {
        return Peer.getLocal().id == originPeer.id
    }


    override fun resolveConflict(conflict: Conflict): Distributed {
        TODO("Not yet implemented")
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DFile

        if (originPeer != other.originPeer) return false
        if (path != other.path) return false

        return true
    }

    override fun hashCode(): Int {
        var result = originPeer.hashCode()
        result = 31 * result + path.hashCode()
        return result
    }


}