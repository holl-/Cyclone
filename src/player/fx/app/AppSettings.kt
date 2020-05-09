package player.fx.app

import com.aquafx_project.AquaFx
import cloud.CloudFile
import cloud.Peer
import javafx.application.Application
import javafx.application.Platform
import javafx.collections.ListChangeListener
import javafx.collections.transformation.FilteredList
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.fxml.Initializable
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.stage.DirectoryChooser
import javafx.stage.Stage
import javafx.stage.Window
import player.model.CycloneConfig
import player.model.PlaylistPlayer
import java.io.File
import java.net.URL
import java.util.*
import java.util.function.Predicate
import java.util.stream.Collectors

class AppSettings(val config: CycloneConfig, var player: PlaylistPlayer) : Initializable {
    var stage: Stage = Stage()

    // General
    @FXML var skin: ComboBox<String>? = null
    @FXML var singleInstance: CheckBox? = null
    // Library
    @FXML var libraryDirectories: ListView<CloudFile>? = null
    // Network
    @FXML var connectOnStartup: CheckBox? = null
    @FXML var computerName: TextField? = null
    @FXML var multicastAddress: TextField? = null
    @FXML var multicastPort: TextField? = null
    @FXML var broadcastInterval: TextField? = null
    @FXML var connectionStatus: Label? = null

    private val windows = FilteredList<Window>(Window.getWindows(), Predicate { w -> w is Stage })
    private var saveDisabled: Boolean = false

    init {
        var loader = FXMLLoader(javaClass.getResource("settings.fxml"))
        loader.setController(this);
        stage.scene = Scene(loader.load())

        windows.addListener( ListChangeListener { change -> while(change.next()) applyStyle(skin!!.selectionModel.selectedItem, change.addedSubList) })
    }


    override fun initialize(location: URL?, resources: ResourceBundle?) {
        // --- Skin ---
        skin!!.items.setAll("Modena", "Caspian", "AquaFX", "Dark")
        updateUIValues()

        skin!!.selectionModel.selectedItemProperty().addListener { _, _, newStyle -> applyStyle(newStyle, windows)}
        Platform.runLater {applyStyle(skin!!.selectionModel.selectedItem, windows)}

        // --- Single instance ---
        // --- Library ---
        libraryDirectories!!.items = player.library.roots
        if(!player.library.roots.isEmpty()) {
            libraryDirectories!!.selectionModel.select(0)
        }

        connectionStatus!!.textProperty().bind(player.cloud.connectionStatus)

        // --- Save on Change ---
        for (property in listOf(
                connectOnStartup!!.selectedProperty(),
                singleInstance!!.selectedProperty(),
                player.library.roots,
                computerName!!.textProperty(),
                multicastAddress!!.textProperty(),
                multicastPort!!.textProperty(),
                broadcastInterval!!.textProperty())) {
            property.addListener { _ -> save()}
        }

        // --- control listeners --- ToDo this will be saved in serialized form in the future using Cloud.write
        player.loopingProperty.addListener{_ -> save()}
        player.shuffledProperty.addListener{_ -> save()}
        player.gainProperty.addListener{_ -> save()}
    }

    fun updateUIValues() {
        skin!!.selectionModel.select(config.getString("skin", "Modena"))
        singleInstance!!.selectedProperty().set(config.getString("singleInstance", "true").toBoolean())

        // --- Network ---
        connectOnStartup!!.isSelected = config.getString("connectOnStartup", "false").toBoolean()
        computerName!!.text = Peer.getLocal().name
        multicastAddress!!.text = config.getString("multicastAddress", "225.139.25.1")
        multicastPort!!.text = config.getString("multicastPort", "5324")
        broadcastInterval!!.text = config.getString("broadcastRate", "1")

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
        save()
    }


    private fun save() {
        if (saveDisabled) return
        config.update(mapOf<String, Any>(
                "skin" to skin!!.selectionModel.selectedItem,
                "singleInstance" to singleInstance!!.isSelected,
                "looping" to player.loopingProperty.get(),
                "shuffled" to player.shuffledProperty.get(),
                "gain" to player.gainProperty.get(),
                "library" to player.library.roots.stream().map { f -> f.getPath() }.collect(Collectors.joining("; ")),
                "connectOnStartup" to connectOnStartup!!.isSelected,
                "computerName" to computerName!!.text,
                "multicastAddress" to multicastAddress!!.text,
                "multicastPort" to multicastPort!!.text,
                "broadcastRate" to broadcastInterval!!.text
        ))
        config.write()
    }

    @FXML
    private fun removeLibraryRoot() {
        if(libraryDirectories!!.selectionModel.selectedItem != null)
            player.library.roots.remove(libraryDirectories!!.selectionModel.selectedItem)
        save()
    }

    @FXML
    private fun addLibraryRoot() {
        val chooser = DirectoryChooser()
        val dir: File? = chooser.showDialog(stage)
        if(dir != null) {
            player.library.roots.add(CloudFile(dir))
            save()
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
        config.properties.clear()
        config.write()
        saveDisabled = true
        updateUIValues()
        saveDisabled = false
    }

}