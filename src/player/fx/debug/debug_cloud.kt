package player.fx.debug

import cloud.Cloud
import cloud.SynchronizedData
import javafx.application.Platform
import javafx.beans.InvalidationListener
import javafx.collections.FXCollections
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.fxml.Initializable
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.Label
import javafx.scene.control.ListView
import javafx.scene.control.ToggleButton
import javafx.scene.layout.VBox
import javafx.stage.Stage
import java.net.URL
import java.util.*


class CloudSnapshot(val index: Int, val sync: List<SynchronizedData>) {
    override fun toString(): String {
        return index.toString()
    }
}


class CloudViewer(val cloud: Cloud) : Initializable
{
    val stage: Stage = Stage()

    @FXML private var live: ToggleButton? = null
    @FXML private var snapshotView: ListView<CloudSnapshot>? = null

    private val snapshots = FXCollections.observableArrayList<CloudSnapshot>()
    private var snapshotsCreated = 0

    @FXML var synchronizedView: VBox? = null

    init {
        val loader = FXMLLoader(javaClass.getResource("cloud-viewer.fxml"))
        loader.setController(this)
        val root = loader.load<Parent>()

        stage.scene = Scene(root)
        stage.title = "Cyclone Data"
        stage.x = 0.0
        stage.y = 500.0

        takeSnapshot()
    }

    override fun initialize(p0: URL?, p1: ResourceBundle?) {
        cloud.onUpdate.add(Runnable { takeSnapshot() })
        snapshotView?.items = snapshots
        snapshotView?.selectionModel?.selectedItemProperty()?.addListener(InvalidationListener { rebuild() })
    }

    @FXML fun clearSnapshots() {
        if(snapshots.size > 1)
            snapshots.removeAll(snapshots.subList(0, snapshots.size - 1))
    }

    private fun takeSnapshot() {
        val snapshot = CloudSnapshot(snapshotsCreated, cloud.getAllCurrentSynchronized())
        snapshotsCreated++
        snapshots.add(snapshot)
        if(live?.isSelected == true) {
            snapshotView?.selectionModel?.select(snapshot)
        }
    }


    @FXML private fun rebuild() {
        synchronizedView!!.children.clear()

        val snapshot = snapshotView!!.selectionModel.selectedItem

        for (data in snapshot.sync) {
            val node = Label(data.toString())
            node.isWrapText = true
            synchronizedView!!.children.add(node)
        }
    }

}