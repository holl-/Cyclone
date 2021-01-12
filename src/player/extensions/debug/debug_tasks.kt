package player.extensions.debug

import cloud.Cloud
import cloud.CloudFile
import cloud.Peer
import javafx.application.Platform
import javafx.beans.InvalidationListener
import javafx.collections.FXCollections
import javafx.collections.ListChangeListener
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.fxml.Initializable
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.layout.VBox
import javafx.stage.Stage
import player.extensions.CycloneExtension
import player.fx.icons.FXIcons
import player.model.data.PlayTask
import player.model.data.PlayTaskStatus
import player.model.data.Speaker
import player.model.data.TaskTrigger
import java.io.File
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.net.URL
import java.util.*
import kotlin.collections.ArrayList


class TaskDebuggingExtension : CycloneExtension("Debug: Tasks", "View scheduled audio files and their status.", "same as Cyclone", true, false) {
    var viewer: TaskViewer? = null

    override fun activate(cloud: Cloud) {
        viewer = TaskViewer(cloud)
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


class Snapshot(val index: Int, val tasks: List<PlayTask>, val statuses: List<PlayTaskStatus>)
{
    override fun toString(): String {
        return index.toString()
    }
}


class TaskViewer(val cloud: Cloud) : Initializable
{
    val tasks = cloud.getAll(PlayTask::class.java, this, Platform::runLater)
    val statuses = cloud.getAll(PlayTaskStatus::class.java, this, Platform::runLater)

    private val root: Parent
    private var stage: Stage? = null

    @FXML private var immediateTasks: VBox? = null
    @FXML private var scheduledTasks: VBox? = null
    @FXML private var deletedTasks: VBox? = null
    @FXML private var live: ToggleButton? = null
    @FXML private var snapshotView: ListView<Snapshot>? = null

    private val snapshots = FXCollections.observableArrayList<Snapshot>()
    private var snapshotsCreated = 0

    init {
        val loader = FXMLLoader(javaClass.getResource("task-viewer.fxml"))
        loader.setController(this)
        root = loader.load<Parent>()


        takeSnapshot()

        val fakeSpeaker = Speaker(Peer.getLocal(), "debug", "Fake Speaker", -10.0, 20.0, false)
        cloud.push(Speaker::class.java, listOf(fakeSpeaker), this, false)
    }

    override fun initialize(p0: URL?, p1: ResourceBundle?) {
        tasks.addListener { _: ListChangeListener.Change<out PlayTask> -> takeSnapshot() }
        statuses.addListener { _: ListChangeListener.Change<out PlayTaskStatus> -> takeSnapshot() }
        snapshotView?.items = snapshots
        snapshotView?.selectionModel?.selectedItemProperty()?.addListener(InvalidationListener { rebuild() })
    }

    fun show(stage: Stage) {
        stage.scene = Scene(root)
        stage.x = 0.0
        stage.y = 0.0
        this.stage = stage
        stage.show()
    }

    @FXML fun createTask() {
        val creator = TaskCreator(cloud)
        creator.stage.x = stage!!.x + stage!!.width
        creator.stage.y = stage!!.y
        creator.stage.show()
    }

    @FXML fun clearSnapshots() {
        if(snapshots.size > 1)
            snapshots.removeAll(snapshots.subList(0, snapshots.size - 1))
    }

    private fun takeSnapshot() {
        val snapshot = Snapshot(snapshotsCreated, ArrayList(tasks), ArrayList(statuses))
        snapshotsCreated++
        snapshots.add(snapshot)
        if(live?.isSelected == true) {
            snapshotView?.selectionModel?.select(snapshot)
        }
    }

    private fun rebuild() {
        immediateTasks?.children?.clear();
        scheduledTasks?.children?.clear();
        deletedTasks?.children?.clear();

        val snapshot = snapshotView!!.selectionModel.selectedItem

        for(task in snapshot.tasks) {
            val stat = snapshot.statuses.filter { status -> status.task == task }
            if(task.trigger == null) {
                immediateTasks?.children?.add(createTaskNode(task, stat))
            } else {
                scheduledTasks?.children?.add(createTaskNode(task, stat))
            }
        }
        for(status in snapshot.statuses) {
            if (status.task !in snapshot.tasks) {
                deletedTasks?.children?.add(createTaskNode(status.task, listOf(status)))
            }
        }
    }


    private fun createTaskNode(task: PlayTask, statuses: List<PlayTaskStatus>): Node {
        val node = VBox()
        val label = TaskView(task, statuses)
        node.children.add(label.root)
        return node
    }
}


class TaskCreator(val cloud: Cloud) : Initializable
{
    val stage: Stage = Stage()

    @FXML private var file: TextField? = null
    @FXML private var target: ComboBox<Speaker>? = null
    @FXML private var trigger: TextField? = null
    @FXML private var startPosition: TextField? = null
    @FXML private var duration: TextField? = null
    @FXML private var resetCount: TextField? = null

    @FXML private var gain: Slider? = null
    @FXML private var balance: Slider? = null
    @FXML private var paused: CheckBox? = null
    @FXML private var mute: CheckBox? = null

    @FXML private var id: TextField? = null
    @FXML private var creator: TextField? = null

    private var task : PlayTask? = null

    companion object {
        private var creatorCount = 0
    }

    init {
        val loader = FXMLLoader(javaClass.getResource("task-creator.fxml"))
        loader.setController(this)
        val root = loader.load<Parent>()

        stage.scene = Scene(root)
        stage.title = "Create Task"
        stage.setOnCloseRequest { yank() }

        creatorCount++
    }

    override fun initialize(p0: URL?, p1: ResourceBundle?) {
        file?.text = "C:\\stereo.mp3"
        target?.items = cloud.getAll(Speaker::class.java, this, Platform::runLater)
        target?.selectionModel?.select(target?.items?.firstOrNull { speaker -> speaker.isDefault })
        duration?.text = "5"
        startPosition?.text = "10"
        id?.text = creatorCount.toString()
        creator?.text = "user"
        resetCount?.text = "0"
    }


    @FXML fun createTaskAfter() {
        val creator = TaskCreator(cloud)
        creator.stage.x = stage.x + stage.width
        creator.stage.y = stage.y
        creator.stage.show()
        Platform.runLater { creator.trigger?.text = id?.text }
    }

    @FXML fun submit() {
        if(target?.selectionModel?.selectedItem == null) return
        task = PlayTask(
                target = target!!.selectionModel.selectedItem,
                file = CloudFile(File(file!!.text)),
                gain = gain!!.value,
                mute = mute!!.isSelected,
                balance = balance!!.value,
                position = if(startPosition!!.text.isEmpty()) 0.0 else startPosition!!.text.toDouble(),
                restartCount = resetCount!!.text.toInt(),
                duration = if(duration!!.text.isEmpty()) null else duration!!.text.toDouble(),
                creator = creator!!.text,
                paused = paused!!.isSelected,
                trigger = if(trigger!!.text.isEmpty()) null else TaskTrigger(trigger!!.text),
                id = id!!.text
        )
        cloud.push(PlayTask::class.java, listOf(task!!), this, true)
    }

    @FXML fun yank() {
        cloud.yankAll(null, this)
    }

}


class TaskView(val task: PlayTask, val statuses: List<PlayTaskStatus>) : Initializable {
    @FXML private var title: TitledPane? = null
    @FXML private var file: Label? = null
    @FXML private var filePeer: Label? = null
    @FXML private var target: Label? = null
    @FXML private var targetPeer: Label? = null
    @FXML private var position: Label? = null
    @FXML private var duration: Label? = null
    @FXML private var restart: Label? = null
    @FXML private var trigger: Label? = null
    @FXML var statusesList: VBox? = null

    var root: Node

    init {
        val loader = FXMLLoader(javaClass.getResource("task.fxml"))
        loader.setController(this)
        root = loader.load<Parent>()
    }

    override fun initialize(p0: URL?, p1: ResourceBundle?) {
        title!!.text = task.id
        file!!.text = task.file.getPath()
        filePeer!!.text = task.file.getOrigin().name
        target!!.text = task.target.name
        targetPeer!!.text = task.target.peer.name
        position!!.text = "Position: ${task.position}"
        duration!!.text = "Duration: ${task.duration?.toString() ?: "undefined"}"
        restart!!.text = "Restarted: ${task.restartCount} times"
        trigger!!.text = task.trigger?.taskId ?: "immediately"
        file!!.graphic = if(task.paused) FXIcons.get("Pause.png", 20.0) else FXIcons.get("Play.png", 20.0)
        for (status in statuses) {
            val statusLabel = Label("${status.displayString()}, position=${status.task.position}, duration=${status.task.duration}")
            statusLabel.isWrapText = true
            statusLabel.graphic = if(status.task.paused) FXIcons.get("Pause.png", 20.0) else FXIcons.get("Play.png", 20.0)
            statusesList!!.children.add(statusLabel)
        }
    }

}

