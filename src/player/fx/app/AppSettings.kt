package player.fx.app

import com.aquafx_project.AquaFx
import javafx.application.Application
import javafx.application.Platform
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.fxml.Initializable
import javafx.scene.Scene
import javafx.scene.control.CheckBox
import javafx.scene.control.ComboBox
import javafx.stage.Stage
import player.model.CyclonePlayer
import java.io.File
import java.net.URL
import java.util.*
import java.util.stream.Collectors

class AppSettings(var player: CyclonePlayer) : Initializable {

    @FXML var skin: ComboBox<String>? = null
    @FXML var singleInstance: CheckBox? = null
    var stage: Stage = Stage()
    var stylableStages: List<Stage> = mutableListOf(stage)
    val settingsFile: File = File(System.getProperty("user.home") + "/AppData/Roaming/Cyclone/settings.txt").absoluteFile

    init {
        var loader = FXMLLoader(javaClass.getResource("settings.fxml"))
        loader.setController(this);
        stage.scene = Scene(loader.load())
    }


    override fun initialize(location: URL?, resources: ResourceBundle?) {
        val settings = load()

        skin!!.items.addAll("Modena", "Caspian", "AquaFX", "Dark")
        skin!!.selectionModel.select(settings.getOrDefault("skin", "Modena"))
        skin!!.selectionModel.selectedItemProperty().addListener { _, _, newStyle -> setStyle(newStyle)}
        Platform.runLater {setStyle(skin!!.selectionModel.selectedItem)}

        singleInstance!!.selectedProperty().set(settings.getOrDefault("singleInstance", "true").toBoolean())
        singleInstance!!.selectedProperty().addListener { _ -> save()}

        player.isLoop = settings.getOrDefault("looping", "true").toBoolean()
        player.loopProperty().addListener{_ -> save()}
        player.isShuffled = settings.getOrDefault("shuffled", "false").toBoolean()
        player.shuffledProperty().addListener{_ -> save()}
        player.gain = settings.getOrDefault("gain", "0.0").toDouble()
        player.gainProperty().addListener{_ -> save()}
    }


    fun setStyle(style: String) {
        when (style) {
            "AquaFX" -> AquaFx.style()
            "Dark" -> {
                Application.setUserAgentStylesheet("MODENA")
                for(stage in stylableStages) {
                    stage.scene.getStylesheets().clear()
                    stage.scene.getStylesheets().add(javaClass.getResource("dark.css").toExternalForm())
                }
            }
            else -> {
                for(stage in stylableStages) {
                    stage.scene.getStylesheets().clear()
                }
                Application.setUserAgentStylesheet(style.toUpperCase())
            }
        }
        save()
    }


    private fun save() {
        if(!settingsFile.parentFile.exists())
            settingsFile.parentFile.mkdirs()
        val properties = mapOf<String, Any>(
                "skin" to skin!!.selectionModel.selectedItem,
                "singleInstance" to singleInstance!!.isSelected,
                "looping" to player.isLoop,
                "shuffled" to player.isShuffled,
                "gain" to player.gain
        )

        settingsFile.printWriter().use { out ->
            properties.forEach {
                out.println("${it.key}: ${it.value}\n")
            }
        }
    }


    private fun load(): Map<String, String> {
        return if(settingsFile.exists()) {
            settingsFile.readLines().parallelStream()
                    .filter{line -> !line.trim().isEmpty()}
                    .collect(Collectors.toMap(
                        {line -> line.substring(0, line.indexOf(':')).trim()},
                        {line -> line.substring(line.indexOf(':') + 1).trim()}
            ))
        } else {
            emptyMap()
        }

    }

}