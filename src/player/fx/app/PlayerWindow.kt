package player.fx.app

import audio.UnsupportedMediaFormatException
import cloud.CloudFile
import javafx.animation.FadeTransition
import javafx.animation.TranslateTransition
import javafx.application.Platform
import javafx.beans.binding.Bindings
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.beans.value.ObservableValue
import javafx.collections.FXCollections
import javafx.collections.ListChangeListener
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.fxml.Initializable
import javafx.geometry.Insets
import javafx.scene.Node
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.control.Alert.AlertType
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.input.MouseButton
import javafx.scene.input.MouseEvent
import javafx.scene.layout.*
import javafx.scene.paint.Color
import javafx.stage.Modality
import javafx.stage.Stage
import javafx.stage.WindowEvent
import javafx.util.Callback
import javafx.util.Duration
import player.fx.FileDropOverlay
import player.fx.PlayerControl
import player.fx.control.SpeakerCell
import player.fx.debug.CloudViewer
import player.fx.debug.PlaybackViewer
import player.fx.debug.TaskViewer
import player.fx.icons.FXIcons
import player.model.AudioFiles
import player.model.CycloneConfig
import player.model.MediaLibrary
import player.model.PlaylistPlayer
import player.model.data.Speaker
import player.model.playback.Job
import player.model.playback.PlaybackEngine
import java.awt.Desktop
import java.io.File
import java.io.IOException
import java.net.URL
import java.util.*
import java.util.concurrent.Callable
import java.util.function.Function
import java.util.stream.Collectors


class PlayerWindow internal constructor(private val stage: Stage, val statusWrapper: PlaylistPlayer, // can be null
                                        private val engine: PlaybackEngine, config: CycloneConfig?) : Initializable {
    private val root: StackPane
    private var currentOverlay: Node? = null

    // Default
    @FXML private var currentSongMenu: Menu? = null
    @FXML private var settingsMenu: Menu? = null
    @FXML private var addToLibraryMenu: Menu? = null
    @FXML private var debugMenu: Menu? = null
    @FXML private var cannotAddToLibraryItem: MenuItem? = null
    @FXML private var menuBar: MenuBar? = null
    @FXML private var volume: Slider? = null
    @FXML private var speakerSelection: ComboBox<Speaker>? = null

    // Playlist
    private val playlistRoot: Node
    private var playlistListView: Node? = null

    // Search
    private val searchRoot: Node
    private var searchFocus: Node? = null
    private val library: MediaLibrary
    private val settings: AppSettings


    override fun initialize(location: URL?, resources: ResourceBundle?) {
        settingsMenu!!.text = null
        settingsMenu!!.graphic = FXIcons.get("Settings.png", 24.0)
        currentSongMenu!!.graphic = FXIcons.get("Media.png", 24.0)
        currentSongMenu!!.textProperty().bind(statusWrapper.titleProperty)
        currentSongMenu!!.disableProperty().bind(statusWrapper.isFileSelectedProperty.not())
        volume!!.valueProperty().bindBidirectional(statusWrapper.gainProperty)
        speakerSelection!!.items = statusWrapper.speakers
        speakerSelection!!.selectionModel.select(statusWrapper.speakerProperty.get())
        speakerSelection!!.selectionModel.selectedItemProperty().addListener { _, _, n -> if (n != null) statusWrapper.speakerProperty.set(n) }
        speakerSelection!!.setCellFactory { SpeakerCell() }
        statusWrapper.speakerProperty.addListener { _, _, n -> speakerSelection!!.selectionModel.select(n) }
        if (settings.config["debug"] != "true") debugMenu!!.parentMenu.items.remove(debugMenu)
    }


    @Throws(IOException::class)
    private fun loadPlayer(): BorderPane {
        val loader = FXMLLoader(javaClass.getResource("mp3player.fxml"))
        loader.setController(this)
        val playerRoot = loader.load<BorderPane>()
        val control = PlayerControl()
        control.durationProperty().bind(statusWrapper.durationProperty)
        control.positionProperty().bindBidirectional(statusWrapper.positionProperty)
        control.playingProperty().bindBidirectional(statusWrapper.playingProperty)
        control.mediaSelectedProperty().bind(statusWrapper.isFileSelectedProperty)
        control.playlistAvailableProperty().bind(statusWrapper.playlistAvailableProperty)
        control.shuffledProperty().bindBidirectional(statusWrapper.shuffledProperty)
        control.loopProperty().bindBidirectional(statusWrapper.loopingProperty)
        control.onNext = EventHandler { e: ActionEvent? -> statusWrapper.next() }
        control.onPrevious = EventHandler { e: ActionEvent? -> statusWrapper.previous() }
        control.onStop = EventHandler { e: ActionEvent? -> statusWrapper.stop() }
        control.onShowPlaylist = EventHandler { e: ActionEvent? -> showPlaylist() }
        control.onSearch = EventHandler { e: ActionEvent? -> showSearch() }
        playerRoot.center = control

//		control.setOnMouseDragged(e -> e.);
        return playerRoot
    }

    @Throws(IOException::class)
    private fun loadPlaylist(): BorderPane {
        val loader = FXMLLoader(javaClass.getResource("playlist.fxml"))
        loader.setController(object : Initializable {
            @FXML private var removeOthersButton: Button? = null
            @FXML private var playlist: ListView<CloudFile>? = null

            override fun initialize(location: URL?, resources: ResourceBundle?) {
                playlist!!.setItems(statusWrapper.playlist)
                playlist!!.addEventFilter(KeyEvent.KEY_PRESSED, TabAndEnterHandler(playlist))
                removeOthersButton!!.disableProperty().bind(Bindings.createBooleanBinding(
                        Callable { statusWrapper.playlist.size == 0 || statusWrapper.playlist.size == 1 && statusWrapper.currentFileProperty.get() === statusWrapper.playlist[0] },
                        statusWrapper.playlist, statusWrapper.currentFileProperty))
                playlist!!.selectionModel.selectedItemProperty().addListener { p: ObservableValue<out CloudFile?>?, o: CloudFile?, n: CloudFile? ->
                    if (n != null) {
                        statusWrapper.currentFileProperty.set(n)
                        statusWrapper.playingProperty.set(true)
                    }
                }
                statusWrapper.currentFileProperty.addListener { p: ObservableValue<out CloudFile?>?, o: CloudFile?, n: CloudFile? -> playlist!!.selectionModel.select(statusWrapper.currentFileProperty.get()) }
                playlist!!.setCellFactory { list: ListView<CloudFile?>? -> MediaCell() }
                playlistListView = playlist
            }

            @FXML
            fun closePlaylist() {
                fadeOut(playlistRoot)
            }

            @FXML
            fun clearPlaylist() {
                statusWrapper.currentFileProperty.set(null)
                statusWrapper.playlist.clear()
                closePlaylist()
            }

            @FXML
            fun clearOthers() {
                val newList: MutableList<CloudFile> = ArrayList()
                statusWrapper.currentFileProperty.value?.let { current -> newList.add(current) }
                statusWrapper.setPlaylist(newList)
            }
        })
        val playlistRoot = loader.load<BorderPane>()
        playlistRoot.background = Background(BackgroundFill(Color(0.0, 0.0, 0.0, 0.5), CornerRadii.EMPTY, Insets.EMPTY))
        return playlistRoot
    }

    @Throws(IOException::class)
    private fun loadSearch(): BorderPane {
        val loader = FXMLLoader(javaClass.getResource("search.fxml"))
        loader.setController(object : Initializable {
            @FXML private var searchResult: ListView<CloudFile>? = null
            @FXML private var searchField: TextField? = null

            override fun initialize(location: URL?, resources: ResourceBundle?) {
                searchField!!.textProperty().addListener { p: ObservableValue<out String>?, o: String?, n: String ->
                    if (n.isEmpty()) {
                        searchResult!!.setItems(library.recentlyUsed)
                    } else {
                        searchResult!!.setItems(library.startSearch(n))
                    }
                    if (!searchResult!!.items.isEmpty()) searchResult!!.selectionModel.select(0)
                    searchResult!!.items.addListener(ListChangeListener { change: ListChangeListener.Change<out CloudFile>? -> if (!searchResult!!.items.isEmpty()) searchResult!!.selectionModel.select(0) })
                }
                searchResult!!.setItems(library.recentlyUsed)
                searchResult!!.setCellFactory { list: ListView<CloudFile>? -> MediaCell() }
                searchResult!!.onKeyPressed = EventHandler { e: KeyEvent -> if (e.code == KeyCode.ENTER) playSelected(e.isControlDown) }
                searchField!!.onKeyPressed = EventHandler { e: KeyEvent ->
                    if (e.code == KeyCode.ENTER) {
                        playSelected(e.isControlDown)
                        e.consume()
                    } else if (e.code == KeyCode.DOWN) {
                        val next = searchResult!!.selectionModel.selectedIndex + 1
                        if (searchResult!!.items.size > next) searchResult!!.selectionModel.select(next)
                        e.consume()
                    } else if (e.code == KeyCode.UP) {
                        val prev = searchResult!!.selectionModel.selectedIndex - 1
                        if (prev >= 0) searchResult!!.selectionModel.select(prev)
                        e.consume()
                    }
                }
                searchResult!!.onMouseReleased = EventHandler { e: MouseEvent -> if (e.button == MouseButton.PRIMARY) playSelected(e.isControlDown) }
                searchFocus = searchField
            }

            @FXML
            fun closeSearch() {
                fadeOut(searchRoot)
            }

            private fun playSelected(append: Boolean) {
                val m = searchResult!!.selectionModel.selectedItem
                m?.let { playFromLibrary(it, append) }
                Platform.runLater { closeSearch() }
            }
        })
        val searchRoot = loader.load<BorderPane>()
        searchRoot.background = Background(BackgroundFill(Color(0.0, 0.0, 0.0, 0.5), CornerRadii.EMPTY, Insets.EMPTY))
        return searchRoot
    }

    private fun generateDropButtons(files: List<File>): List<ToggleButton> {
        val result: MutableList<ToggleButton> = ArrayList(3)
        val audioFiles = AudioFiles.trim(AudioFiles.unfold(files))
        val cold = statusWrapper.playlist.isEmpty()

        // Play / New Playlist
        if (!audioFiles.isEmpty()) {
            val play = ToggleButton("Play", FXIcons.get("Play2.png", 32.0))
            play.onAction = EventHandler { e: ActionEvent? -> play(audioFiles, files[0]) }
            result.add(play)
        }

        // Add to Playlist
        if (!cold && !audioFiles.isEmpty()) {
            val append = ToggleButton("Add to playlist", FXIcons.get("Append.png", 32.0))
            append.onAction = EventHandler { e: ActionEvent? ->
                val dfiles = audioFiles.stream().map { file: File? -> CloudFile(file!!) }.collect(Collectors.toList())
                statusWrapper.addToPlaylist(dfiles)
                if (statusWrapper.currentFileProperty.get() == null) {
                    statusWrapper.currentFileProperty.set(dfiles[0])
                }
            }
            result.add(append)
        }

        // Play Folder
        if (files.size == 1) {
            val file = files[0]
            val allAudioFiles = AudioFiles.allAudioFilesIn(files[0].parentFile)
            if (allAudioFiles.size > 1) {
                val playFolder = ToggleButton("Play folder", FXIcons.get("PlayFolder.png", 32.0))
                playFolder.onAction = EventHandler { e: ActionEvent? -> play(allAudioFiles, if (AudioFiles.isAudioFile(file)) file else allAudioFiles[0]) }
                result.add(playFolder)
            }
        }
        return result
    }

    fun play(localFiles: List<File>, startFile: File?) {
        val remoteFiles = localFiles.stream().map { file: File? -> CloudFile(file!!) }.collect(Collectors.toList())
        //		for(DFile file : remoteFiles) {
//			if(!player.getLibrary().isIndexed(file)) {
//				player.getLibrary().getOrAdd(file);
//			}
//		}
        statusWrapper.setPlaylist(remoteFiles)
        statusWrapper.currentFileProperty.set(CloudFile(startFile!!))
        statusWrapper.playingProperty.set(true)
    }

    private fun fadeIn(node: Node, focus: Node?) {
        if (root.children.contains(node)) {
            fadeOut(node)
            return
        }
        if (currentOverlay != null) {
            fadeOut(currentOverlay!!)
        }
        currentOverlay = node
        root.children.add(node)
        val time = Duration(200.0)
        val `in` = TranslateTransition(time, node)
        `in`.fromY = -20.0
        `in`.toY = 0.0
        `in`.play()
        val fade = FadeTransition(time, node)
        fade.fromValue = 0.0
        fade.toValue = 1.0
        fade.play()
        focus!!.requestFocus()
    }

    private fun fadeOut(node: Node) {
        val time = Duration(200.0)
        val `in` = TranslateTransition(time, node)
        `in`.fromY = 0.0
        `in`.toY = -10.0
        `in`.play()
        val fade = FadeTransition(time, node)
        fade.fromValue = 1.0
        fade.toValue = 0.0
        fade.play()
        fade.onFinished = EventHandler { e: ActionEvent? -> root.children.remove(node) }
        currentOverlay = null
    }

    fun showPlaylist() {
        fadeIn(playlistRoot, playlistListView)
    }

    @FXML
    fun showSearch() {
        fadeIn(searchRoot, searchFocus)
    }

    private fun updateAddToLibraryMenu() {
        addToLibraryMenu!!.items.clear()
        addToLibraryMenu!!.isDisable = true
        if (statusWrapper.currentFileProperty.get() != null) {
            val file = statusWrapper.currentFileProperty.get()
            if (file!!.originatesHere()) {
                var localFile: File? = File(file.getPath()).absoluteFile
                while (localFile != null && library.isIndexed(CloudFile(localFile))) {
                    localFile = localFile.parentFile
                }
                if (localFile != null) {
                    do {
                        val finalFile = localFile!!
                        val item = MenuItem(if (localFile.name.isEmpty()) localFile.absolutePath else localFile.name)
                        item.graphic = FXIcons.get(if (localFile.isDirectory) "PlayFolder.png" else "Media.png", 28.0)
                        item.onAction = EventHandler { e: ActionEvent? -> library.roots.add(CloudFile(finalFile)) }
                        addToLibraryMenu!!.items.add(item)
                        localFile = localFile.parentFile
                    } while (localFile != null)
                    addToLibraryMenu!!.isDisable = false
                }
            }
        }
        if (addToLibraryMenu!!.items.isEmpty()) {
            addToLibraryMenu!!.items.setAll(Arrays.asList(cannotAddToLibraryItem))
        }
    }

    private fun playFromLibrary(file: CloudFile, append: Boolean) {
        val files: List<CloudFile>
        files = if (file.isDirectory()) {
            val allFiles = AudioFiles.unfold(Arrays.asList(File(file.getPath())))
            allFiles.stream().filter { file: File? -> AudioFiles.isAudioFile(file) }.map { file: File? -> CloudFile(file!!) }.collect(Collectors.toList())
        } else Arrays.asList(file)
        if (!append) {
            statusWrapper.setPlaylist(files)
            statusWrapper.currentFileProperty.set(files[0])
        } else {
            statusWrapper.addToPlaylist(files)
            if (statusWrapper.currentFileProperty.get() == null) {
                statusWrapper.currentFileProperty.set(files[0])
            }
        }
    }

    internal class MediaCell : ListCell<CloudFile>() {
        var fileIcon = FXIcons.get("Play.png", 32.0)
        var dirIcon = FXIcons.get("PlayFolder.png", 32.0)

        override fun updateItem(item: CloudFile?, empty: Boolean) {
            super.updateItem(item, empty)
            if (item != null) {
                text = AudioFiles.inferTitle(item.getPath())
                graphic = if (item.isDirectory()) dirIcon else fileIcon
            } else {
                text = null
                graphic = null
            }
        }
    }

    fun show() {
        stage.show()
        stage.width = 314.0 // default values, apply for bundled application
        stage.height = 402.0
    }

    @FXML
    fun quit() {
        System.exit(0)
    }

    @FXML
    fun displayInfo() {
        val info = Alert(AlertType.INFORMATION)
        info.title = "Info"
        info.headerText = "Cyclone"
        info.contentText = "Version 0.2\nAuthor: Philipp Holl\nMay 2020"
        info.initOwner(stage)
        info.initModality(Modality.NONE)
        info.show()
    }

    @FXML
    fun openFileLocation() {
        openFileLocation(statusWrapper.currentFileProperty.get())
    }

    private fun openFileLocation(file: CloudFile?) {
        if (file != null && file.originatesHere()) {
            try {
                Desktop.getDesktop().browse(File(file.getPath()).parentFile.toURI())
            } catch (e: NoSuchElementException) {
                e.printStackTrace()
                Alert(AlertType.ERROR, "Could not open location: " + file.getPath(), ButtonType.OK).show()
            } catch (e: IOException) {
                e.printStackTrace()
                Alert(AlertType.ERROR, "Could not open location: " + file.getPath(), ButtonType.OK).show()
            }
        } else {
            Alert(AlertType.INFORMATION, "The file is not located on this systemcontrol.", ButtonType.OK).show()
        }
    }

    @FXML
    fun removeCurrentFromPlaylist() {
        statusWrapper.removeCurrentFileFromPlaylist()
    }

    internal inner class TabAndEnterHandler(private val node: Node?) : EventHandler<KeyEvent> {
        override fun handle(event: KeyEvent) {
            val parent = node!!.parent
            if (event.code == KeyCode.ENTER || event.code == KeyCode.ESCAPE) {
                parent.fireEvent(event.copyFor(parent, parent))
                event.consume()
            }
        }

    }

    @FXML
    @Throws(IOException::class)
    fun showFileInfo() {
        val file = statusWrapper.currentFileProperty.get() ?: return
        val loader = FXMLLoader(javaClass.getResource("fileinfo.fxml"))
        loader.setController(object : Initializable {
            @FXML
            private val encodingTab: Tab? = null

            @FXML
            private val playbackTab: Tab? = null

            @FXML
            private val titleLabel: Label? = null

            @FXML
            private val durationLabel: Label? = null

            @FXML
            private val encodingLabel: Label? = null

            @FXML
            private val pathLink: Hyperlink? = null

            @FXML
            private val propertiesTable: TableView<Map.Entry<String, Any>>? = null

            @FXML
            private val propertyColumn: TableColumn<Map.Entry<String?, Any?>, String?>? = null

            @FXML
            private val valueColumn: TableColumn<Map.Entry<String?, Any?>, Any?>? = null

            @FXML
            private val eEncoding: Label? = null

            @FXML
            private val eChannels: Label? = null

            @FXML
            private val eSampleRate: Label? = null

            @FXML
            private val eSampleSize: Label? = null

            @FXML
            private val eFrameSize: Label? = null

            @FXML
            private val eFrameRate: Label? = null

            @FXML
            private val eEndianness: Label? = null

            @FXML
            private val eProperties: Label? = null

            @FXML
            private val dEncoding: Label? = null

            @FXML
            private val dChannels: Label? = null

            @FXML
            private val dSampleRate: Label? = null

            @FXML
            private val dSampleSize: Label? = null

            @FXML
            private val dFrameSize: Label? = null

            @FXML
            private val dFrameRate: Label? = null

            @FXML
            private val dEndianness: Label? = null

            @FXML
            private val dProperties: Label? = null

            @FXML
            private val playbackEngine: Label? = null
            override fun initialize(location: URL?, resources: ResourceBundle?) {
                val opJob = engine.jobs.stream().filter { j: Job -> j.task.get() != null && j.task.get()!!.file === file && j.player.get() != null }.findFirst()
                if (opJob.isPresent) {
                    val player = opJob.get().player.get()!!
                    try {
                        player.loadMediaFormat()
                    } catch (e: IOException) {
                        e.printStackTrace()
                        val alert = Alert(AlertType.ERROR)
                        alert.title = "File error"
                        alert.headerText = "Failed to retrieve media information."
                        alert.contentText = e.message
                        alert.showAndWait()
                        return
                    } catch (e: UnsupportedMediaFormatException) {
                        e.printStackTrace()
                        val alert = Alert(AlertType.ERROR)
                        alert.title = "File error"
                        alert.headerText = "Failed to retrieve media information."
                        alert.contentText = e.message
                        alert.showAndWait()
                        return
                    }
                    val file = player.mediaFile
                    val format = player.mediaFormat
                    val info = player.mediaInfo
                    titleLabel!!.text = if (info.title != null) info.title else file.fileName
                    pathLink!!.text = file.file.absolutePath
                    durationLabel!!.text = "Duration: " + info.duration + " seconds / " + format.frameLength + " frames"
                    encodingLabel!!.text = "File type: " + format.type.name + ", size: " + file.fileSize / 1024 / 1024 + " MB (" + file.fileSize + " bytes)"
                    propertiesTable!!.items.addAll(FXCollections.observableArrayList<Map.Entry<String, Any>>(format.properties.entries))
                    val ef = player.encodedFormat
                    eEncoding!!.text = "Encoding: " + ef.encodingName
                    eChannels!!.text = "Channels: " + if (ef.channels == 2) "Stereo" else if (ef.channels == 1) "Mono" else ef.channels
                    eSampleRate!!.text = "Sample rate: " + ef.sampleRate + " Hz"
                    eSampleSize!!.text = "Sample size: " + if (ef.sampleSizeInBits > 0) ef.sampleSizeInBits.toString() + " bits" else "variable"
                    eFrameSize!!.text = "Frame size: " + if (ef.frameSize > 0) ef.frameSize.toString() + " bytes" else "variable"
                    eFrameRate!!.text = "Frame rate: " + ef.frameRate + " Hz"
                    eEndianness!!.text = "Endianness: " + if (ef.isBigEndian) "big endian" else "little endian"
                    eProperties!!.text = if (ef.properties.isEmpty()) "" else ef.properties.toString()
                    playbackEngine!!.text = "Playback engine: " + format.audioEngineName
                    val df = player.decodedFormat
                    dEncoding!!.text = "Encoding: " + df.encodingName
                    dChannels!!.text = "Channels: " + if (df.channels == 2) "Stereo" else if (df.channels == 1) "Mono" else df.channels
                    dSampleRate!!.text = "Sample rate: " + df.sampleRate + " Hz"
                    dSampleSize!!.text = "Sample size: " + if (df.sampleSizeInBits > 0) df.sampleSizeInBits.toString() + " bits" else "variable"
                    dFrameSize!!.text = "Frame size: " + if (df.frameSize > 0) df.frameSize.toString() + " bytes" else "variable"
                    dFrameRate!!.text = "Frame rate: " + df.frameRate + " Hz"
                    dEndianness!!.text = "Endianness: " + if (df.isBigEndian) "big endian" else "little endian"
                    dProperties!!.text = if (df.properties.isEmpty()) "" else df.properties.toString()
                } else {
                    titleLabel!!.text = file.getName()
                    pathLink!!.text = file.getPath()
                    durationLabel!!.text = "Media details are unavailable because file is not stored locally."
                    encodingTab!!.isDisable = true
                    playbackTab!!.isDisable = true
                }
                propertyColumn!!.cellValueFactory = Callback { entry: TableColumn.CellDataFeatures<Map.Entry<String?, Any?>, String?> -> SimpleStringProperty(entry.value.key) }
                valueColumn!!.cellValueFactory = Callback { entry: TableColumn.CellDataFeatures<Map.Entry<String?, Any?>, Any?> -> SimpleObjectProperty(entry.value.value) }
            }

            @FXML
            protected fun showFolder() {
                openFileLocation(file)
            }
        })
        val playerRoot = loader.load<BorderPane>()
        val stage = Stage()
        stage.title = "Media Info"
        stage.scene = Scene(playerRoot)
        stage.show()
    }

    @FXML
    fun showSettings() {
        settings.stage.show()
    }

    @FXML
    fun openTaskViewer() {
        val stage = Stage()
        val viewer = TaskViewer(statusWrapper.cloud, stage)
        viewer.stage.show()
    }

    @FXML
    fun openPlaybackWindow() {
        val viewer = PlaybackViewer(engine)
        viewer.stage.show()
    }

    @FXML
    fun openCloudViewer() {
        val viewer = CloudViewer(statusWrapper.cloud, null)
        viewer.stage.show()
    }

    init {
        library = statusWrapper.library
        settings = AppSettings(config!!, statusWrapper)
        root = StackPane()
        root.children.add(loadPlayer())
        playlistRoot = loadPlaylist()
        searchRoot = loadSearch()
        statusWrapper.currentFileProperty.addListener { p: ObservableValue<out CloudFile?>?, o: CloudFile?, n: CloudFile? -> updateAddToLibraryMenu() }
        val overlay = FileDropOverlay(root)
        overlay.actionGenerator = Function { files: List<File> -> generateDropButtons(files) }
        var scene: Scene?
        stage.scene = Scene(root).also { scene = it }
        stage.title = "Cyclone"
        stage.icons.add(FXIcons.get("Play2.png", 32.0).image)
        stage.onHidden = EventHandler { e: WindowEvent? -> quit() }
    }
}