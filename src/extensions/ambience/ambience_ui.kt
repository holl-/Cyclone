package extensions.ambience

import cloud.Cloud
import cloud.CloudFile
import extensions.CycloneExtension
import javafx.application.Platform
import javafx.beans.binding.Bindings
import javafx.beans.property.DoubleProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.value.ChangeListener
import javafx.collections.FXCollections
import javafx.collections.ListChangeListener
import javafx.collections.ObservableList
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.fxml.Initializable
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.control.cell.TextFieldListCell
import javafx.scene.input.KeyCode
import javafx.scene.layout.StackPane
import javafx.scene.layout.VBox
import javafx.stage.FileChooser
import javafx.stage.Stage
import javafx.util.Callback
import javafx.util.StringConverter
import player.CastToDoubleProperty
import player.CustomObjectProperty
import player.fx.control.SpeakerCell
import player.fx.icons.FXIcons
import player.model.PlayerData
import player.model.data.MasterGain
import player.model.data.Speaker
import java.io.File
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.net.URL
import java.util.*
import java.util.concurrent.Callable
import java.util.function.Consumer
import java.util.function.Supplier


class AmbienceExtension : CycloneExtension("Ambience", "Lets you configure ambient sounds effects.", "0.1", true)
{
    private var window: AmbienceApp? = null
    private var settings: AmbienceSettings? = null

    override fun activate(cloud: Cloud) {
        settings = AmbienceSettings(cloud)
        window = AmbienceApp(settings!!, cloud, this)
    }

    override fun deactivate() {
        window = null
    }

    override fun load(stream: ObjectInputStream) {
        settings!!.load(stream)
        window!!.load(stream)
    }

    override fun save(stream: ObjectOutputStream) {
        settings!!.save(stream)
        window!!.save(stream)
    }

    override fun show(stage: Stage) {
        window!!.show(stage)
    }

    override fun settings(): Node? {
        return settings
    }

}


class AmbienceApp(val settings: AmbienceSettings, val cloud: Cloud, val extension: AmbienceExtension) : Initializable {
    private val root: Parent

    private val playlistData = cloud.getSynchronized(PlayerData.Playlist::class.java, Supplier { PlayerData.Playlist(emptyList()) })
    val playlist: ObservableList<CloudFile> = FXCollections.observableArrayList(playlistData.value.files)
    private val gainData = cloud.getSynchronized(MasterGain::class.java, default = Supplier { MasterGain(0.0) })
    val gainProperty: DoubleProperty = CastToDoubleProperty(CustomObjectProperty<Number?>(listOf(gainData),
            getter = Supplier<Number?> { gainData.value?.value ?: 0 },
            setter = Consumer { value -> cloud.pushSynchronized(MasterGain(value!!.toDouble())) }))

    val pauseIcon = FXIcons.get("Pause.png", 20.0)
    val playIcon = FXIcons.get("Play.png", 20.0)

    @FXML private var playing: ToggleButton? = null
    @FXML private var masterVolume: Slider? = null
    @FXML private var ambiences: ListView<Ambience>? = null
    @FXML private var ambiencePane: VBox? = null

    internal var stage: Stage? = null

    init {
        val loader = FXMLLoader(javaClass.getResource("ambience.fxml"))
        loader.setController(this)
        root = loader.load()
        Platform.runLater { if (ambiences!!.items.isEmpty()) createAmbience() }
        playlistData.addListener(ChangeListener{ _, _, _ -> playlist.setAll(playlistData.value?.files ?: emptyList())})
    }

    fun show(stage: Stage) {
        this.stage = stage
        stage.scene = Scene(root)
        stage.show()
    }


    override fun initialize(p0: URL?, p1: ResourceBundle?) {
        playing!!.graphicProperty().bind(Bindings.createObjectBinding(Callable { if (playing!!.isSelected) pauseIcon else playIcon }, playing!!.selectedProperty()))
        masterVolume!!.valueProperty().bindBidirectional(gainProperty)
        ambiences!!.items = FXCollections.observableArrayList()
        ambiences!!.cellFactory = TextFieldListCell.forListView(AmbienceNameConverter(ambiences!!))
        ambiences!!.selectionModel.selectedItemProperty().addListener { _, _, ambience -> ambience?.let { ambiencePane!!.children.setAll(ambience.effects) } ?: ambiencePane!!.children.clear()  }
        ambiences!!.setOnKeyPressed { e -> if(e.code == KeyCode.DELETE) deleteAmbience() }
    }

    @FXML fun createEffect() {
        ambiences!!.selectionModel.selectedItem?.createEffect()
    }

    @FXML fun createAmbience() {
        val newAmbience = Ambience(this)
        ambiences!!.items.add(newAmbience)
        ambiences!!.selectionModel.select(newAmbience)
    }

    fun deleteAmbience() {
        ambiences!!.selectionModel.selectedItem?.let { ambience -> ambiences!!.items.remove(ambience) }
    }

    fun save(stream: ObjectOutputStream) {
        stream.writeInt(ambiences!!.items.size)
        for (ambience in ambiences!!.items) {
            ambience.write(stream)
        }
    }

    fun load(stream: ObjectInputStream) {
        ambiences!!.items.clear()
        for (index in 1..stream.readInt()) {
            val ambience = Ambience(this)
            ambience.load(stream)
            ambiences!!.items.add(ambience)
            ambiences!!.selectionModel.select(ambience)
        }
    }
}

private class AmbienceNameConverter(val listView: ListView<Ambience>): StringConverter<Ambience>() {
    override fun toString(ambience: Ambience?): String? {
        return ambience?.name
    }

    override fun fromString(name: String?): Ambience? {
        return name?.let { listView.selectionModel.selectedItem.name = name; listView.selectionModel.selectedItem }
    }
}



class Ambience(val window: AmbienceApp, var name: String = "New ambience") {
    val effects = VBox()

    init {
        effects.spacing = 8.0
        createEffect()
    }

    fun createEffect() {
        effects.children.add(EffectPane(this))
    }

    fun write(stream: ObjectOutputStream) {
        stream.writeUTF(name)
        stream.writeInt(effects.children.size)
        for (effect in effects.children) {
            (effect as EffectPane).write(stream)
        }
    }

    fun load(stream: ObjectInputStream) {
        effects.children.clear()
        name = stream.readUTF()
        for (index in 1..stream.readInt()) {
            val effect = EffectPane(this)
            effect.read(stream)
            effects.children.add(effect)
        }
    }
}


class EffectPane(val ambience: Ambience) : StackPane(), Initializable {
    val root: TitledPane
    val file = SimpleObjectProperty<File>()

    @FXML var selectFromPlaylist: ComboBox<CloudFile>? = null
    @FXML var fileDisplay: Label? = null
    @FXML var gain: Slider? = null
    @FXML var direction: ComboBox<SoundSource>? = null
    @FXML var continuous: CheckBox? = null
    @FXML var nTimes: TextField? = null
    @FXML var perTimeUnit: ComboBox<String>? = null

    init {
        val loader = FXMLLoader(javaClass.getResource("effect.fxml"))
        loader.setController(this)
        root = loader.load()
        root.isExpanded = false
        children.add(root)
        Platform.runLater { root.isExpanded = true }
    }

    override fun initialize(p0: URL?, p1: ResourceBundle?) {
        fileDisplay!!.textProperty().bind(Bindings.createStringBinding(Callable { file.value?.name }, file))
        perTimeUnit!!.items = FXCollections.observableArrayList("minute", "10 minutes", "hour")
        perTimeUnit!!.selectionModel.select("10 minutes")

        nTimes!!.textProperty().addListener { _, _, text ->
            if (!text.matches(Regex("\\d*"))) {
                nTimes!!.text = text.replace("[^\\d]".toRegex(), "")
            }
        }
        nTimes!!.disableProperty().bind(continuous!!.selectedProperty())
        perTimeUnit!!.disableProperty().bind(continuous!!.selectedProperty())
        selectFromPlaylist!!.items = ambience.window.playlist
        selectFromPlaylist!!.selectionModel.selectedItemProperty().addListener { _ ->
            selectFromPlaylist!!.selectionModel.selectedItem?.let {
                if (it.originatesHere()) {
                    file.value = File(it.getPath())
                    selectFromPlaylist!!.selectionModel.select(null)
                } else {
                    Alert(Alert.AlertType.WARNING, "You can only choose files that are stored on this computer.").show()
                }
            }
        }
        direction!!.items = ambience.window.settings.soundSources
        direction!!.buttonCell = SoundSourceCell()
        direction!!.cellFactory = Callback { SoundSourceCell() }
    }

    @FXML fun selectFile() {
        val chooser = FileChooser()
        val chosenFile: File? = chooser.showOpenDialog(ambience.window.stage!!)
        chosenFile?.let { file.value = chosenFile }
    }

    @FXML fun deleteEffect() {
        file.value = null
        (parent as VBox).children.remove(this)
    }

    fun write(stream: ObjectOutputStream) {
        stream.writeUTF(file.value?.absolutePath ?: "")
        stream.writeDouble(gain!!.value)
        stream.writeUTF(direction!!.selectionModel.selectedItem?.toString() ?: "")
        stream.writeBoolean(continuous!!.isSelected)
        stream.writeUTF(nTimes!!.text)
        stream.writeUTF(perTimeUnit!!.selectionModel.selectedItem)
    }

    fun read(stream: ObjectInputStream) {
        val path = stream.readUTF()
        if (path.isNotEmpty()) file.value = File(path)
        gain!!.value = stream.readDouble()
        val directionName = stream.readUTF()
        val sources = ambience.window.settings.soundSources
        val directionIndex = sources.indexOfFirst { s -> s.toString() == directionName }
        if (directionIndex >= 0) direction!!.selectionModel.select(directionIndex)
        continuous!!.isSelected = stream.readBoolean()
        nTimes!!.text = stream.readUTF()
        if (nTimes!!.text.isBlank()) nTimes!!.text = "1"
        perTimeUnit!!.selectionModel.select(stream.readUTF())
    }

    private class SoundSourceCell : ListCell<SoundSource>()
    {
        override fun updateItem(item: SoundSource?, empty: Boolean) {
            super.updateItem(item, empty)
            if (item != null) {
                text = item.name!!.text
            } else {
                text = null
            }
        }
    }
}


class AmbienceSettings(val cloud: Cloud) : StackPane(), Initializable {
    @FXML private var sources: VBox? = null

    val soundSources = FXCollections.observableArrayList<SoundSource>()

    init {
        val loader = FXMLLoader(javaClass.getResource("ambience-settings.fxml"))
        loader.setController(this)
        val root = loader.load<Parent>()
        children.add(root)

        soundSources.addListener(ListChangeListener { sources!!.children.setAll(soundSources) })
    }

    override fun initialize(p0: URL?, p1: ResourceBundle?) {

    }

    @FXML fun createSource() {
        soundSources.add(SoundSource(cloud, this))
    }

    fun save(stream: ObjectOutputStream) {
        stream.writeInt(soundSources.size)
        for (source in soundSources) {
            (source as SoundSource).save(stream)
        }
    }

    fun load(stream: ObjectInputStream) {
        soundSources.clear()
        for (index in 1..stream.readInt()) {
            val source = SoundSource(cloud, this)
            source.load(stream)
            soundSources.add(source)
        }
    }
}


class SoundSource(val cloud: Cloud, val settings: AmbienceSettings) : StackPane(), Initializable
{
    private val speakers = cloud.getAll(Speaker::class.java)

    @FXML var name: TextField? = null
    @FXML private var speaker: ComboBox<Speaker>? = null
    @FXML private var balance: Slider? = null


    init {
        val loader = FXMLLoader(javaClass.getResource("sound-source.fxml"))
        loader.setController(this)
        val root = loader.load<Parent>()
        children.add(root)
    }

    override fun initialize(p0: URL?, p1: ResourceBundle?) {
        speaker!!.items = speakers
        speaker!!.setCellFactory { SpeakerCell() }
        speaker!!.buttonCell = SpeakerCell()
        name!!.text = "Source ${settings.soundSources.size + 1}"
    }

    @FXML fun deleteSource() {
        settings.soundSources.remove(this)
    }

    fun save(stream: ObjectOutputStream) {
        stream.writeUTF(name!!.text)
        stream.writeUTF(speaker!!.selectionModel.selectedItem?.name ?: "")
        stream.writeUTF(speaker!!.selectionModel.selectedItem?.peer?.name ?: "")
        stream.writeDouble(balance!!.value)
    }

    fun load(stream: ObjectInputStream) {
        name!!.text = stream.readUTF()
        stream.readUTF()  // speaker name
        stream.readUTF()  // speaker peer
        balance!!.value = stream.readDouble()
    }

    override fun toString(): String {
        return name?.text ?: "Illegal"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SoundSource) return false

        if (toString() != other.toString()) return false

        return true
    }

    override fun hashCode(): Int {
        return toString().hashCode()
    }


}