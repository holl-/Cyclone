package player.fx.app

import com.aquafx_project.AquaFx
import cloud.CloudFile
import javafx.application.Application
import javafx.application.Platform
import javafx.collections.ListChangeListener
import javafx.collections.transformation.FilteredList
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.fxml.Initializable
import javafx.scene.Scene
import javafx.scene.control.CheckBox
import javafx.scene.control.ComboBox
import javafx.scene.control.ListView
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
    // General
    @FXML var skin: ComboBox<String>? = null
    @FXML var singleInstance: CheckBox? = null
    // Library
    @FXML var libraryDirectories: ListView<CloudFile>? = null

    var stage: Stage = Stage()
    private val windows = FilteredList<Window>(Window.getWindows(), Predicate { w -> w is Stage })

    init {
        var loader = FXMLLoader(javaClass.getResource("settings.fxml"))
        loader.setController(this);
        stage.scene = Scene(loader.load())

        windows.addListener( ListChangeListener { change -> while(change.next()) applyStyle(skin!!.selectionModel.selectedItem, change.addedSubList) })
    }


    override fun initialize(location: URL?, resources: ResourceBundle?) {
        // --- Skin ---
        skin!!.items.addAll("Modena", "Caspian", "AquaFX", "Dark")
        skin!!.selectionModel.select(config.getString("skin", "Modena"))
        skin!!.selectionModel.selectedItemProperty().addListener { _, _, newStyle -> applyStyle(newStyle, windows)}
        Platform.runLater {applyStyle(skin!!.selectionModel.selectedItem, windows)}
        // --- Single instance ---
        singleInstance!!.selectedProperty().set(config.getString("singleInstance", "true").toBoolean())
        singleInstance!!.selectedProperty().addListener { _ -> save()}
        // --- control listeners --- ToDo this will be saved in serialized form in the future using Cloud.write
        player.loopingProperty.addListener{_ -> save()}
        player.shuffledProperty.addListener{_ -> save()}
        player.gainProperty.addListener{_ -> save()}
        // --- Library ---
        libraryDirectories!!.items = player.library.roots
        if(!player.library.roots.isEmpty()) {
            libraryDirectories!!.selectionModel.select(0)
        }
        player.library.roots.addListener(ListChangeListener { e -> save() })
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
        config.update(mapOf<String, Any>(
                "skin" to skin!!.selectionModel.selectedItem,
                "singleInstance" to singleInstance!!.isSelected,
                "looping" to player.loopingProperty.get(),
                "shuffled" to player.shuffledProperty.get(),
                "gain" to player.gainProperty.get(),
                "library" to player.library.roots.stream().map { f -> f.getPath() }.collect(Collectors.joining("; "))
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

}