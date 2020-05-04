package player

import java.io.File


fun getConfigFile(filename: String): File {
    return File(filename)
}