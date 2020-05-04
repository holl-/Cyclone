package distributed

import distributed.internal.ListDirRequest
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.util.*
import java.util.stream.Stream

class DFile(file: File) : Distributed(false, true) {
    private val path: String = file.path
    private var size: Long? = null  // read upon serialization
    private var isDir: Boolean? = null // read upon serialization


    fun getPath(): String {
        return path
    }

    fun getName() : String {
        val parts = path.split("/|\\\\")
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
     * [DistributedPlatform]. Therefore this method also returns a valid size if the
     * hosting peer is not available.
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
            return platform.query(ListDirRequest(this), origin)
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
            return platform.openStream(origin, getPath())
        }
    }

    /**
     * A file originates here if its associated peer is the local peer.
     *
     * @return if this file is stored locally
     * @see Peer.isLocal
     */
    fun originatesHere(): Boolean {
        return Peer.getLocal().id == origin.id
    }


    override fun toString(): String {
        return path
    }


    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DFile

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