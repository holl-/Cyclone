package player.model.playback

import audio.MediaFile
import audio.Player
import cloud.CloudFile
import javafx.beans.property.SimpleObjectProperty
import javafx.collections.FXCollections
import java.io.File
import java.io.InputStream
import java.net.URI

class DMediaFile(val file: CloudFile) : MediaFile {
    // For resource management, this class would be ideal.
//    var players = FXCollections.observableArrayList<Player>()
//    val currentJobs = FXCollections.observableArrayList<Job>()


    override fun getFile(): File? {
        return if(file.originatesHere()) File(file.getPath()) else null
    }

    override fun getFileName(): String {
        return file.getName()
    }

    override fun getFileSize(): Long {
        return file.length()
    }

    override fun toURI(): URI? {
        return if(file.originatesHere()) File(file.getPath()).toURI() else null
    }

    override fun openStream(): InputStream {
        return file.openStream()
    }

    override fun toString(): String {
        return fileName
    }
}