package player.model

import java.io.File
import java.io.IOException
import java.util.*


fun getConfigFile(filename: String): File {
    return File(System.getProperty("user.home") + "/AppData/Roaming/Cyclone/" + filename).absoluteFile
}

class CycloneConfig(val file: File) {
    val properties = Properties()


    fun getString(key: String, defaultValue: String): String {
        return properties.getOrDefault(key, defaultValue) as String
    }

    operator fun get(key: String): String? {
        return properties[key] as String?
    }

    fun update(values: Map<String, Any>) {
        for (entry in values) {
            properties[entry.key] = entry.value.toString()
        }
    }

    fun write() {
        if(!file.parentFile.exists())
            file.parentFile.mkdirs()
        properties.store(file.printWriter(), null)
    }

    fun read() {
        properties.load(file.bufferedReader())
    }


    companion object {
        private val GLOBAL_CONFIG = CycloneConfig(getConfigFile("settings.txt"))
        private var INITIALIZED = false

        fun getGlobal(): CycloneConfig {
            if(!INITIALIZED) {
                try {
                    GLOBAL_CONFIG.read()
                } catch (exc: IOException) {}
                INITIALIZED = true
            }
            return GLOBAL_CONFIG
        }
    }
}