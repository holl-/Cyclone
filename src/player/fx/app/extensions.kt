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


class ExtensionInfo(val extension: CycloneExtension, initiallyEnabled: Boolean, val initialAutoShow: Boolean, val cloud: Cloud) : StackPane(), Initializable {
    val root: TitledPane

    val saveFile = getConfigFile("ext_${extension.name}.obj")

    @FXML private var enabled: CheckBox? = null
    @FXML private var description: Label? = null
    @FXML private var version: Label? = null
    @FXML private var showNow: Button? = null
    @FXML private var showOnStartup: CheckBox? = null
    @FXML private var reset: Button? = null
    @FXML private var settings: StackPane? = null

    private var extensionStage: Stage? = null

    init {
        val loader = FXMLLoader(javaClass.getResource("extension-info.fxml"))
        loader.setController(this)
        root = loader.load()
        root.text = extension.name
        children.add(root)

        if (initiallyEnabled) Platform.runLater { enable() }

        Platform.runLater { if (initialAutoShow) show() }
    }

    override fun initialize(p0: URL?, p1: ResourceBundle?) {
        showNow!!.disableProperty().bind(Bindings.createBooleanBinding(Callable { !enabled!!.isSelected || !extension.canShow }, enabled!!.selectedProperty()))
        showOnStartup!!.disableProperty().bind(showNow!!.disableProperty())
        showOnStartup!!.isSelected = initialAutoShow
        reset!!.isDisable = !extension.canSave
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
        if (!enabled!!.isSelected)
            return

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

    @FXML fun reset() {
        val wasShowing = extensionStage?.isShowing == true
        val wasEnabled = enabled!!.isSelected
        enabled!!.isSelected = false  // this hides the window
        extensionStage = null
        saveFile.delete()
        enabled!!.isSelected = wasEnabled
        if (wasShowing) show()
    }

    fun enable() {
        enabled!!.isSelected = true
    }

    fun isEnabled(): Boolean {
        return enabled?.isSelected == true
    }

    fun enabledProperty(): BooleanProperty {
        return enabled!!.selectedProperty()
    }

    fun isAutoShow(): Boolean {
        return showOnStartup!!.isSelected
    }

    fun autoShowProperty(): BooleanProperty {
        return showOnStartup!!.selectedProperty()
    }

    fun save() {
        ObjectOutputStream(saveFile.outputStream()).use {
            extension.save(it)
        }
    }

    fun load() {
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