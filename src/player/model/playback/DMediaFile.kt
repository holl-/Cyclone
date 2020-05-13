package player.model.playback

import audio.MediaFile
import cloud.CloudFile
import java.io.File
import java.io.InputStream
import java.net.URI

class DMediaFile(val file: CloudFile) : MediaFile {
    // For resource management, this class would be ideal.
//    var players = FXCollections.observableArrayList<Player>()
//    val currentJobs = FXCollections.observableArrayList<Job>()

    private var localFile: File? = if (file.originatesHere()) File(file.getPath()) else null


    override fun getFile(): File? {
        return localFile
    }

    override fun getFileName(): String {
        return file.getName()
    }

    override fun getFileSize(): Long {
        return file.length()
    }

    override fun toURI(): URI? {
        return localFile?.toURI()
    }

    override fun openStream(): InputStream {
        synchronized(this) {
            if (localFile == null) {
                localFile = File.createTempFile("stream_", file.getName())
                localFile!!.outputStream().use { fstream ->
                    file.openStream().use {
                        it.transferTo(fstream)
                    }
                }
            }
            return localFile!!.inputStream()
        }
    }

    override fun toString(): String {
        return fileName
    }
}


class MediaFileManager() {
    private val fileMap = HashMap<CloudFile, DMediaFile>()

    fun get(file: CloudFile): DMediaFile {
        return fileMap[file] ?: run {
            val newFile = DMediaFile(file)
            fileMap[file] = newFile
            newFile
        }
    }
}