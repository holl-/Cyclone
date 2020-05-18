package player.fx.app

import cloud.Cloud
import player.extensions.CycloneExtension
import javafx.application.Platform
import javafx.beans.binding.Bindings
import javafx.beans.property.BooleanProperty
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.fxml.Initializable
import javafx.scene.control.Button
import javafx.scene.control.CheckBox
import javafx.scene.control.Label
import javafx.scene.control.TitledPane
import javafx.scene.layout.StackPane
import javafx.stage.Stage
import player.model.getConfigFile
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.lang.Exception
import java.lang.IllegalStateException
import java.net.URL
import java.util.*
import java.util.concurrent.Callable


class ExtensionInfo(val extension: CycloneExtension, initiallyEnabled: Boolean, val cloud: Cloud) : StackPane(), Initializable {
    val root: TitledPane

    @FXML private var enabled: CheckBox? = null
    @FXML private var description: Label? = null
    @FXML private var version: Label? = null
    @FXML private var showNow: Button? = null
    @FXML private var settings: StackPane? = null

    private var extensionStage: Stage? = null

    init {
        val loader = FXMLLoader(javaClass.getResource("extension-info.fxml"))
        loader.setController(this)
        root = loader.load()
        root.text = extension.name
        children.add(root)

        if (initiallyEnabled) Platform.runLater { enable() }
    }

    override fun initialize(p0: URL?, p1: ResourceBundle?) {
        showNow!!.disableProperty().bind(Bindings.createBooleanBinding(Callable { !enabled!!.isSelected || !extension.canShow }, enabled!!.selectedProperty()))
        description!!.text = extension.description
        version!!.text = extension.version
        enabled!!.selectedProperty().addListener { _, _, enabledV ->
            if (enabled!!.isSelected) {
                extension.activate(cloud)
                load()
                extension.settings()?.let { node -> settings!!.children.setAll(node) }
//                show()
            } else {
                extensionStage?.close()
                settings!!.children.clear()
                save()
                extension.deactivate()
            }
        }
    }

    @FXML fun show() {
        if (!enabled!!.isSelected) throw IllegalStateException("must be enabled before showing")

        if (extensionStage?.isShowing == true) {
            extensionStage!!.toFront()
            return
        }
        if (extensionStage != null) {
            extensionStage!!.show()
            return
        }
        extensionStage = Stage()
        extensionStage!!.setOnCloseRequest { save() }
        extension.show(extensionStage!!)
    }

    fun enable() {
        enabled?.isSelected = true
    }

    fun isEnabled(): Boolean {
        return enabled?.isSelected == true
    }

    fun enabledProperty(): BooleanProperty {
        return enabled!!.selectedProperty()
    }

    fun save() {
        val saveFile = getConfigFile("ext_${extension.name}.obj")
        ObjectOutputStream(saveFile.outputStream()).use {
            extension.save(it)
        }
    }

    fun load() {
        val saveFile = getConfigFile("ext_${extension.name}.obj")
        if (!saveFile.exists()) return
        try {
            ObjectInputStream(saveFile.inputStream()).use {
                extension.load(it)
            }
        } catch (exc: Exception) {
            exc.printStackTrace()
        }
    }
}