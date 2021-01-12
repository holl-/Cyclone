package player.model

import cloud.CloudFile
import cloud.getComputerName
import javafx.application.Platform
import javafx.beans.InvalidationListener
import javafx.beans.property.*
import player.CastToStringProperty
import player.CustomObjectProperty
import java.io.File
import java.io.IOException
import java.util.*
import java.util.function.Consumer
import java.util.function.Supplier
import java.util.stream.Collectors


fun getConfigFile(filename: String): File {
    return File(System.getProperty("user.home") + "/AppData/Roaming/Cyclone/" + filename).absoluteFile
}

class CycloneConfig(val file: File)
{
    // General
    val debug = SimpleBooleanProperty(this, "debug", false)
    val singleInstance = SimpleBooleanProperty(this, "singleInstance", true)
    val preventStandby = SimpleBooleanProperty(this, "preventStandby", false)
    val skin = SimpleStringProperty(this, "skin", "")
    // Audio
    val audioEngine = SimpleStringProperty(this, "audioEngine", "")
    val bufferTime = SimpleDoubleProperty(this, "bufferTime", 0.0)
    val fadeOutDuration = SimpleDoubleProperty(this, "fadeOutDuration", 0.0)
    val fadeOutGain = SimpleDoubleProperty(this, "fadeOutGain", 0.0)
    val minGain = SimpleDoubleProperty(this, "minGain", 0.0)
    // Library
    val library = SimpleStringProperty(this, "library", "")
    // Network
    val connectOnStartup = SimpleBooleanProperty(this, "connectOnStartup", false)
    val computerName = SimpleStringProperty(this, "computerName", "")
    val multicastAddress = SimpleStringProperty(this, "multicastAddress", "")
    val multicastPort = SimpleIntegerProperty(this, "multicastPort", 0)
    val multicastPortString = CastToStringProperty(CustomObjectProperty<String>(listOf(multicastPort), Supplier { multicastPort.value.toString() }, Consumer<String?> { v -> multicastPort.value = v!!.toInt() }))
    val broadcastInterval = SimpleDoubleProperty(this, "broadcastInterval", 0.0)
    val broadcastIntervalString = CastToStringProperty(CustomObjectProperty<String>(listOf(broadcastInterval), Supplier { broadcastInterval.value.toString() }, Consumer<String?> { v -> broadcastInterval.value = v!!.toDouble() }))
    // Extensions
    val enabledExtensions = SimpleStringProperty(this, "enabledExtensions", "")
    val autoShowExtensions = SimpleStringProperty(this, "autoShowExtensions", "")
    // Key combinations
    val keyCombinations = SimpleBooleanProperty(this, "keyCombinations", true)

    private val allProperties = listOf(
            debug, keyCombinations, singleInstance, skin, preventStandby,
            audioEngine, bufferTime, fadeOutDuration, fadeOutGain, minGain,
            library,
            connectOnStartup, computerName, multicastAddress, multicastPort, broadcastInterval,
            enabledExtensions, autoShowExtensions
    )

    val hasUnsavedChanges = SimpleBooleanProperty(false)

    init {
        reset()
        for (property in allProperties) {
            property.addListener { _, _, _ -> hasUnsavedChanges.value = true }
        }
        hasUnsavedChanges.addListener(InvalidationListener { Platform.runLater { save() } })
    }

    fun reset() {
        // General
        singleInstance.value = true
        preventStandby.value = false
        skin.value = "Modena"
        // Audio
        audioEngine.value = "java"
        bufferTime.value = 0.2
        fadeOutDuration.value = 2.0
        fadeOutGain.value = 40.0
        minGain.value = -40.0
        // Library
        val music = File(System.getProperty("user.home"), "Music")
        library.value = if (music.isDirectory) music.toString() else ""
        // Network
        connectOnStartup.value = false
        computerName.value = getComputerName()
        multicastAddress.value = "225.139.25.1"
        multicastPort.value = 5324
        broadcastInterval.value = 1.0
        // Extensions
        enabledExtensions.value = ""
        autoShowExtensions.value = ""
    }

    fun save() {
        if (!hasUnsavedChanges.value) return
        if(!file.parentFile.exists())
            file.parentFile.mkdirs()
        val properties = Properties()
        for (property in allProperties) {
            properties[property.name] = property.value.toString()
        }
        file.printWriter().use {
            properties.store(it, null)
        }
        hasUnsavedChanges.value = false
    }

    fun load() {
        val properties = Properties()
        file.bufferedReader().use { properties.load(it) }
        for (property in allProperties) {
            when (property) {
                is IntegerProperty -> properties[property.name]?.let { v -> property.value = (v as String).toInt() }
                is BooleanProperty -> properties[property.name]?.let { v -> property.value = (v as String).toBoolean() }
                is DoubleProperty -> properties[property.name]?.let { v -> property.value = (v as String).toDouble() }
                else -> properties[property.name]?.let { v -> property.value = v as String }
            }
        }
        hasUnsavedChanges.value = false
    }

    fun getLibraryFiles(): List<CloudFile> {
        val roots = library.value.split(";".toRegex()).toTypedArray()
        return roots.map { s -> s.trim() }.filter { s -> s.isNotEmpty() }.map { s -> CloudFile(File(s)) }
    }

    fun setLibraryFiles(files: List<CloudFile>) {
        library.value = files.stream().map { f -> f.getPath() }.collect(Collectors.joining(";"))
    }

    fun getEnabledExtensions(): List<String> {
        val extensions = enabledExtensions.value.split(";".toRegex()).toTypedArray()
        return extensions.map { s -> s.trim() }.filter { s -> s.isNotEmpty() }
    }

    fun setEnabledExtensions(extensions: List<String>) {
        enabledExtensions.value = extensions.stream().collect(Collectors.joining(";"))
    }

    fun getAutoShowExtensions(): List<String> {
        val extensions = autoShowExtensions.value.split(";".toRegex()).toTypedArray()
        return extensions.map { s -> s.trim() }.filter { s -> s.isNotEmpty() }
    }

    fun setAutoShowExtensions(extensions: List<String>) {
        autoShowExtensions.value = extensions.stream().collect(Collectors.joining(";"))
    }


    companion object {
        private val GLOBAL_CONFIG = CycloneConfig(getConfigFile("settings.txt"))
        private var INITIALIZED = false

        fun getGlobal(): CycloneConfig {
            if(!INITIALIZED) {
                try {
                    GLOBAL_CONFIG.load()
                } catch (exc: IOException) {}
                INITIALIZED = true
            }
            return GLOBAL_CONFIG
        }
    }
}