package player.fx.debug

import javafx.application.Platform
import javafx.beans.InvalidationListener
import javafx.beans.binding.Bindings
import javafx.beans.property.SimpleLongProperty
import javafx.beans.value.ObservableStringValue
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.fxml.Initializable
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.Label
import javafx.scene.control.TitledPane
import javafx.scene.layout.VBox
import javafx.stage.Stage
import player.FireLater
import player.fx.icons.FXIcons
import player.model.playback.Job
import player.model.playback.PlaybackEngine
import java.net.URL
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit


class PlaybackViewer(val playback: PlaybackEngine) : Initializable
{
    val stage: Stage = Stage()

    @FXML private var jobView: VBox? = null

    init {
        val loader = FXMLLoader(javaClass.getResource("local-playback.fxml"))
        loader.setController(this)
        val root = loader.load<Parent>()

        stage.scene = Scene(root)
        stage.title = "Local Playback"
        stage.x = 1200.0
        stage.y = 600.0

        Platform.runLater { rebuild() }
    }


    override fun initialize(p0: URL?, p1: ResourceBundle?) {
        playback.jobs.addListener(InvalidationListener { Platform.runLater { rebuild() } })
    }

    private fun rebuild() {
        jobView!!.children.clear()
        for (job in playback.jobs) {
            val view = JobView(job)
            jobView!!.children.add(view.root)
            view.root.prefWidthProperty().bind(jobView!!.widthProperty())
        }
    }
}


private class JobView(val job: Job) : Initializable {
    private val fxJobStatus = FireLater(job.status, Platform::runLater)
    private val fxJobTask = FireLater(job.task, Platform::runLater)
    val root: TitledPane

    private var refresh = SimpleLongProperty()

    @FXML private var title: TitledPane? = null
    @FXML private var file: Label? = null
    @FXML private var playerStatus: Label? = null
    @FXML private var position: Label? = null
    @FXML private var message: Label? = null
    @FXML private var jobStatus: Label? = null

    private val playIcon = FXIcons.get("Play.png", 20.0)
    private val pauseIcon = FXIcons.get("Pause.png", 20.0)
    private val inactiveIcon = FXIcons.get("Loop.png", 20.0)


    init {
        val loader = FXMLLoader(javaClass.getResource("job.fxml"))
        loader.setController(this)
        root = loader.load()

        Executors.newScheduledThreadPool(1).scheduleAtFixedRate({ Platform.runLater { refresh.value = System.nanoTime(); refresh.get() } }, 50, 50, TimeUnit.MILLISECONDS)
    }

    override fun initialize(p0: URL?, p1: ResourceBundle?) {
        title!!.text = job.taskId
        file!!.textProperty().bind(fileName())
        playerStatus!!.textProperty().bind(Bindings.createStringBinding(Callable { getPlayerStatus() }, fxJobStatus, refresh))
        playerStatus!!.graphicProperty().bind(Bindings.createObjectBinding(Callable { getPlayerStatusGraphic() }, fxJobStatus, refresh))
        position!!.textProperty().bind(Bindings.createStringBinding(Callable { "${job.player.value?.position ?: ""}" }, refresh))
        message!!.textProperty().bind(Bindings.createObjectBinding(Callable { job.status.value?.message() }, fxJobStatus))
        jobStatus!!.textProperty().bind(Bindings.createStringBinding(Callable { getJobStatus() }, fxJobStatus, refresh))
    }

    private fun getPlayerStatus(): String {
        val player = job.player.value ?: return "Not initialized"
        if (!player.isPrepared) return "Offline"
        if (!player.isActive) return "Inactive"
        if (player.isPlaying) return "Playing"
        return "Active"
    }

    private fun getPlayerStatusGraphic(): Node? {
        val player = job.player.value ?: return null
        if (!player.isActive) return inactiveIcon
        if (player.isPlaying) return playIcon
        return pauseIcon
    }

    private fun getJobStatus(): String {
        if (!job.isAlive()) return "Dead"
        if (job.finished.value) return "Finished"
        if (job.started.value) return "Running"
        if (job.previous.value != null) return "Waiting on ${job.previous.value!!.taskId}"
        return "Inactive"
    }

    fun fileName(): ObservableStringValue {
        return Bindings.createStringBinding(Callable { job.task.value?.file?.getName() }, fxJobTask)
    }
}