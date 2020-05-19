package player.extensions.debug

import cloud.Cloud
import cloud.Data
import cloud.Peer
import cloud.SynchronizedData
import javafx.application.Platform
import javafx.beans.InvalidationListener
import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.value.ChangeListener
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.fxml.Initializable
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.layout.VBox
import javafx.stage.Stage
import player.FireLater
import player.extensions.CycloneExtension
import player.fx.icons.FXIcons
import player.model.data.MasterGain
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.net.URL
import java.util.*
import java.util.logging.Handler
import java.util.logging.Level
import java.util.logging.LogRecord


class CloudDebuggingExtension : CycloneExtension("Debug: Network & Data", "View network traffic and internal data.", "same as Cyclone", true, false) {
    var viewer: CloudViewer? = null

    override fun activate(cloud: Cloud) {
        viewer = CloudViewer(cloud)
    }

    override fun deactivate() {
        viewer = null
    }

    override fun load(stream: ObjectInputStream) {
    }

    override fun save(stream: ObjectOutputStream) {
    }

    override fun show(stage: Stage) {
        viewer!!.show(stage)
    }

    override fun settings(): Node? {
        return null
    }
}


class CloudSnapshot(val index: Int, val sync: List<SynchronizedData>, val data: Map<Peer, List<Data>>) {
    override fun toString(): String {
        return index.toString()
    }
}


class CloudViewer(val cloud: Cloud) : Initializable
{
    val root: Parent
    val log = DisplayLog()

    // Synchronized
    @FXML private var live: ToggleButton? = null
    @FXML private var snapshotView: ListView<CloudSnapshot>? = null
    @FXML private var sClass: ComboBox<String>? = null
    @FXML private var sValue: TextField? = null
    @FXML var synchronizedView: VBox? = null

    // data
    @FXML private var live1: ToggleButton? = null
    @FXML private var snapshotView1: ListView<CloudSnapshot>? = null
    @FXML var dataView: VBox? = null
    @FXML var dummyData: ToggleButton? = null

    // Connection
    @FXML private var connectionStatus: Label? = null
    @FXML private var multicastAddress: TextField? = null
    @FXML private var multicastPort: TextField? = null
    @FXML private var peers: ListView<Peer>? = null
    @FXML private var autoConnect: CheckBox? = null

    // Log
    @FXML private var recordingLog: ToggleButton? = null
    @FXML private var logView: ListView<LogRecord>? = null
    @FXML private var level: ComboBox<String>? = null

    private val snapshots = FXCollections.observableArrayList<CloudSnapshot>()
    private var snapshotsCreated = 0


    init {
        val loader = FXMLLoader(javaClass.getResource("cloud-viewer.fxml"))
        loader.setController(this)
        root = loader.load<Parent>()

        takeSnapshot()

        cloud.logger.addHandler(log)
    }

    override fun initialize(p0: URL?, p1: ResourceBundle?) {
        cloud.onUpdate.add(Runnable {
            takeSnapshot()
            Platform.runLater { peers?.items?.setAll(cloud.peers) }
        })
        snapshotView?.items = snapshots
        snapshotView1?.items = snapshots
        snapshotView?.selectionModel?.selectedItemProperty()?.addListener(InvalidationListener { rebuild() })
        snapshotView1?.selectionModel?.selectedItemProperty()?.addListener(ChangeListener { _, _, v -> snapshotView!!.selectionModel.select(v) })
        snapshotView?.selectionModel?.selectedItemProperty()?.addListener(ChangeListener { _, _, v -> snapshotView1!!.selectionModel.select(v) })
        peers?.items = FXCollections.observableArrayList(cloud.peers)
        peers?.setCellFactory { PeerCell(cloud) }
        level?.items = FXCollections.observableArrayList("Warning", "Info", "Detailed", "Debug")
        level!!.selectionModel.select(log.level.get())
        log.level.bind(level!!.selectionModel.selectedItemProperty())
        logView?.items = log.filteredList
        logView?.setCellFactory { LogCell() }
        recordingLog!!.selectedProperty().bindBidirectional(log.recording)
        connectionStatus!!.textProperty().bind(FireLater<String>(cloud.connectionStatus, Platform::runLater))
        sClass!!.items = FXCollections.observableArrayList("Master Gain")
        sClass!!.selectionModel.select(0)
        sValue!!.text = "0"
        live1!!.selectedProperty().bindBidirectional(live!!.selectedProperty())
        dummyData!!.selectedProperty().addListener(ChangeListener { _, _, hasDummy -> setDummy(hasDummy) })
        multicastAddress!!.text = "225.139.25.1"
        multicastPort!!.text = "5324"
    }

    fun show(stage: Stage) {
        stage.scene = Scene(root)
        stage.x = 0.0
        stage.y = 500.0
        stage.show()
    }

    @FXML fun clearSnapshots() {
        if(snapshots.size > 1)
            snapshots.removeAll(snapshots.subList(0, snapshots.size - 1))
    }

    @FXML fun clearLog() {
        log.close()
    }

    @FXML fun disconnect() {
        cloud.disconnect()
    }

    @FXML fun connect() {
        cloud.connect(multicastAddress!!.text, multicastPort!!.text.toInt(), autoConnect!!.isSelected)
    }

    private fun takeSnapshot() {
        val snapshot = CloudSnapshot(snapshotsCreated, cloud.getAllCurrentSynchronized(), cloud.allData)
        snapshotsCreated++
        Platform.runLater {
            snapshots.add(snapshot)
            if(live?.isSelected == true) {
                snapshotView?.selectionModel?.select(snapshot)
            }
        }
    }


    private fun rebuild() {
        synchronizedView!!.children.clear()
        dataView!!.children.clear()

        val snapshot = snapshotView!!.selectionModel.selectedItem

        for (data in snapshot.sync) {
            val node = Label(data.toString())
            node.isWrapText = true
            synchronizedView!!.children.add(node)
        }

        for ((peer, data) in snapshot.data) {
            val children = VBox()
            val node = TitledPane(peer.toString(), children)
            for (obj in data) {
                val label = Label(obj.toString())
                label.isWrapText = true
                children.children.add(label)
            }
            dataView!!.children.add(node)
        }
    }

    @FXML fun sPush() {
        val clsStr = sClass!!.selectionModel.selectedItem
        val v = sValue!!.text
        val data: SynchronizedData
        if (clsStr == "Master Gain") {
            data = MasterGain(v.toDouble())
        } else {
            return
        }
        cloud.pushSynchronized(data)
    }

    private fun setDummy(hasDummy: Boolean) {
        if (hasDummy) {
            cloud.push(Dummy::class.java, listOf(Dummy()), this, true)
        } else {
            cloud.yankAll(Dummy::class.java, this)
        }
    }

    private data class Dummy(val random: Double = Math.random()) : Data()

    private class PeerCell(val cloud: Cloud) : ListCell<Peer>()
    {
        override fun updateItem(peer: Peer?, empty: Boolean) {
            super.updateItem(peer, empty)
            if (peer != null) {
                graphic = if (peer.isLocal) FXIcons.get("Stop.png", 20.0)
                else {
                    if(cloud.isConnected(peer)) FXIcons.get("Play.png", 20.0)
                    else FXIcons.get("Pause.png", 20.0)
                }
                text = peer.toString()
            } else {
                text = null
                graphic = null
            }
        }
    }

    private class LogCell : ListCell<LogRecord>()
    {
        override fun updateItem(rec: LogRecord?, empty: Boolean) {
            super.updateItem(rec, empty)
            if (rec != null) {
                text = rec.message
            } else {
                text = null
            }
        }
    }

}


class DisplayLog : Handler()
{
    val level: ObjectProperty<String> = SimpleObjectProperty<String>("Detailed")
    val recording = SimpleBooleanProperty(true)

    val all = ArrayList<LogRecord>()
    val filteredList: ObservableList<LogRecord> = FXCollections.observableArrayList()

    init {
        level.addListener(ChangeListener { _, _, l ->
            filteredList.setAll(all.filter { record -> shown(record, l) })
        })
    }

    override fun publish(record: LogRecord) {
        if (!recording.value) return
        Platform.runLater {
            all.add(record)
            if (shown(record, level.get())) filteredList.add(record)
        }
    }

    override fun flush() {}

    override fun close() {
        Platform.runLater {
            all.clear()
            filteredList.clear()
        }
    }

    private fun shown(record: LogRecord, level: String): Boolean {
        return when (level) {
            "Debug" -> true
            "Info" -> record.level.intValue() >= Level.INFO.intValue()
            "Warning" -> record.level.intValue() >= Level.WARNING.intValue()
            "Detailed" -> record.level.intValue() >= Level.FINE.intValue()
            else -> false
        }
    }

}