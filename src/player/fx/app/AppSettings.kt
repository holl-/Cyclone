package player.fx.app

import cloud.CloudFile
import com.aquafx_project.AquaFx
import javafx.application.Application
import javafx.application.Platform
import javafx.beans.binding.Bindings
import javafx.collections.ListChangeListener
import javafx.collections.transformation.FilteredList
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.fxml.Initializable
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.layout.VBox
import javafx.stage.DirectoryChooser
import javafx.stage.Stage
import javafx.stage.Window
import player.CastToBooleanProperty
import player.CustomObjectProperty
import player.FireLater
import player.extensions.ambience.AmbienceExtension
import player.model.CycloneConfig
import player.model.PlaylistPlayer
import java.io.File
import java.math.BigDecimal
import java.math.RoundingMode
import java.net.URL
import java.util.*
import java.util.concurrent.Callable
import java.util.function.Consumer
import java.util.function.Predicate
import java.util.function.Supplier
import kotlin.math.pow

class AppSettings(val config: CycloneConfig, var player: PlaylistPlayer) : Initializable {
    var stage: Stage = Stage()

    // General
    @FXML var skin: ComboBox<String>? = null
    @FXML var singleInstance: CheckBox? = null
    // Audio
    @FXML var javaSound: RadioButton? = null
    @FXML var javaFXSound: RadioButton? = null
    @FXML var bufferTime: Slider? = null
    @FXML var bufferTimeDisplay: Label? = null
    @FXML var fadeOutDuration: Slider? = null
    @FXML var minGain: Slider? = null
    @FXML var minVolumeDisplay: Label? = null
    // Library
    @FXML var libraryDirectories: ListView<CloudFile>? = null
    // Network
    @FXML var connectOnStartup: CheckBox? = null
    @FXML var computerName: TextField? = null
    @FXML var multicastAddress: TextField? = null
    @FXML var multicastPort: TextField? = null
    @FXML var broadcastInterval: TextField? = null
    @FXML var connectionStatus: Label? = null
    // Extensions
    @FXML var extensions: VBox? = null
    // Key combinations
    @FXML var keyCombinations: CheckBox? = null

    private val windows = FilteredList<Window>(Window.getWindows(), Predicate { w -> w is Stage })
    private var saveDisabled: Boolean = false

    private val isJavaSound = CastToBooleanProperty(CustomObjectProperty<Boolean>(listOf(config.audioEngine),  // needs to be referenced, else it will be garbage collected
            getter = Supplier { config.audioEngine.value == "java" },
            setter = Consumer { v -> if(v == true) config.audioEngine.value = "java" }))
    private val isJavaFXSound = CastToBooleanProperty(CustomObjectProperty<Boolean>(listOf(config.audioEngine),  // needs to be referenced, else it will be garbage collected
            getter = Supplier { config.audioEngine.value == "javafx" },
            setter = Consumer { v -> if(v == true) config.audioEngine.value = "javafx" }))

    init {
        val loader = FXMLLoader(javaClass.getResource("settings.fxml"))
        loader.setController(this);
        stage.scene = Scene(loader.load())
        stage.title = "Cyclone Settings"

        windows.addListener( ListChangeListener { change -> while(change.next()) applyStyle(skin!!.selectionModel.selectedItem, change.addedSubList) })

        config.skin.addListener { _, _, newStyle -> applyStyle(newStyle, windows)}

        Platform.runLater {applyStyle(config.skin.value, windows)}
    }

    override fun initialize(location: URL?, resources: ResourceBundle?) {
        // General
        singleInstance!!.selectedProperty().bindBidirectional(config.singleInstance)
        skin!!.items.setAll("Modena", "Caspian", "AquaFX", "Dark")
        skin!!.selectionModel.select(config.skin.value)
        skin!!.selectionModel.selectedItemProperty().addListener { _, _, v -> config.skin.value = v }
        config.skin.addListener { _, _, v -> skin!!.selectionModel.select(v) }
        // Library
        libraryDirectories!!.items = player.library.roots
        if(!player.library.roots.isEmpty()) {
            libraryDirectories!!.selectionModel.select(0)
        }
        // Audio
        val soundEngineGroup = ToggleGroup()
        javaSound!!.toggleGroup = soundEngineGroup
        javaFXSound!!.toggleGroup = soundEngineGroup
        javaSound!!.selectedProperty().bindBidirectional(isJavaSound)
        javaFXSound!!.selectedProperty().bindBidirectional(isJavaFXSound)
        bufferTime!!.valueProperty().bindBidirectional(config.bufferTime)
        bufferTimeDisplay!!.textProperty().bind(Bindings.createStringBinding(Callable { "(${(bufferTime!!.value * 1000).toInt()} milliseconds)" }, bufferTime!!.valueProperty()))
        bufferTime!!.disableProperty().bind(javaFXSound!!.selectedProperty())
        fadeOutDuration!!.valueProperty().bindBidirectional(config.fadeOutDuration)
        minGain!!.valueProperty().bindBidirectional(config.minGain)
        minVolumeDisplay!!.textProperty().bind(Bindings.createStringBinding(Callable { "${BigDecimal(10.0.pow(config.minGain.value / 20)).setScale(4, RoundingMode.HALF_EVEN)}" }, config.minGain))
        // Network
        connectOnStartup!!.selectedProperty().bindBidirectional(config.connectOnStartup)
        computerName!!.textProperty().bindBidirectional(config.computerName)
        multicastAddress!!.textProperty().bindBidirectional(config.multicastAddress)
        multicastPort!!.textProperty().bindBidirectional(config.multicastPortString)
        broadcastInterval!!.textProperty().bindBidirectional(config.broadcastIntervalString)
        connectionStatus!!.textProperty().bind(FireLater<String>(player.cloud.connectionStatus, Platform::runLater))
        // Extensions
        val knownExtensions = listOf(AmbienceExtension())
        for (extension in knownExtensions) {
            val enabled = extension.name in config.getEnabledExtensions()
            val autoShow = extension.name in config.getAutoShowExtensions()
            val pane = ExtensionInfo(extension, enabled, autoShow, player.cloud)
            pane.enabledProperty().addListener { _, _, _ -> config.setEnabledExtensions(extensions!!.children.map { n -> n as ExtensionInfo }.filter { e -> e.isEnabled() }.map { e -> e.extension.name }) }
            pane.autoShowProperty().addListener { _, _, _ -> config.setAutoShowExtensions(extensions!!.children.map { n -> n as ExtensionInfo }.filter { e -> e.isAutoShow() }.map { e -> e.extension.name }) }
            extensions!!.children.add(pane)
        }
        // Key combinations
        keyCombinations!!.selectedProperty().bindBidirectional(config.keyCombinations)
    }

    fun applyStyle(style: String, windows: List<Window>) {
        when (style) {
            "AquaFX" -> AquaFx.style()
            "Dark" -> {
                Application.setUserAgentStylesheet("MODENA")
                for(stage in windows) {
                    stage.scene.getStylesheets().clear()
                    stage.scene.getStylesheets().add(javaClass.getResource("dark.css").toExternalForm())
                }
            }
            else -> {
                for(stage in windows) {
                    stage.scene.getStylesheets().clear()
                }
                Application.setUserAgentStylesheet(style.toUpperCase())
            }
        }
    }

    @FXML
    private fun removeLibraryRoot() {
        if(libraryDirectories!!.selectionModel.selectedItem != null)
            player.library.roots.remove(libraryDirectories!!.selectionModel.selectedItem)
    }

    @FXML
    private fun addLibraryRoot() {
        val chooser = DirectoryChooser()
        val dir: File? = chooser.showDialog(stage)
        if(dir != null) {
            player.library.roots.add(CloudFile(dir))
        }
    }

    @FXML fun disconnect() {
        player.cloud.disconnect()
    }

    @FXML fun connect() {
        player.cloud.connect(multicastAddress!!.text, multicastPort!!.text.toInt(), true, (broadcastInterval!!.text.toDouble() * 1000).toLong())
    }

    @FXML fun close() {
        stage.hide()
    }

    @FXML fun reset() {
        config.reset()
    }

    fun saveExtensions() {
        for (pane in extensions!!.children) {
            val extension = pane as ExtensionInfo
            if (extension.isEnabled())
                extension.save()
        }
    }

}