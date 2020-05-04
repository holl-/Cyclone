package player.model

import audio.MediaFile
import distributed.DFile
import java.io.File
import java.io.InputStream
import java.net.URI

class DMediaFile(val file: DFile) : MediaFile {

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
}