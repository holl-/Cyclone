package player.fx.app;

import audio.*;
import com.aquafx_project.AquaFx;
import com.sun.javafx.css.StyleManager;
import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import player.fx.FileDropOverlay;
import player.fx.PlayerControl;
import player.fx.icons.FXIcons;
import player.fx.playerwrapper.MediaIndexWrapper;
import player.fx.playerwrapper.PlayerStatusWrapper;
import player.model.*;
import player.model.MediaInfo;
import player.playback.PlaybackEngine;
import player.playback.PlayerStatus;
import player.playback.Speaker;
import vdp.RemoteFile;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class PlayerWindow implements Initializable {
	private Scene scene;
	private Stage stage;
	private StackPane root;

	// Default
	@FXML private Menu currentSongMenu, settingsMenu, addToLibraryMenu;
	@FXML private MenuItem cannotAddToLibraryItem;
	@FXML private MenuBar menuBar;
	@FXML private Slider volume;
	@FXML private ComboBox<Speaker> speakerSelection;

	// Playlist
	private Pane playlistRoot;
	@FXML private Button removeOthersButton;
	@FXML private ListView<MediaInfo> playlist;

	// Search
	private Pane searchRoot;
	@FXML private ListView<MediaInfo> searchResult;
	@FXML private TextField searchField;

	private PlayerControl control;

	private PlayerStatus status;
	private PlayerStatusWrapper properties;
	private MediaIndexWrapper index;
	private Node currentOverlay;

	private PlaybackEngine engine;
	private AppSettings settings;


	public PlayerWindow(Stage stage, PlayerStatus status, MediaIndex index, PlaybackEngine engine) throws IOException {
		this.stage = stage;
		this.status = status;
		properties = new PlayerStatusWrapper(status, index);
		this.index = new MediaIndexWrapper(index);
		this.engine = engine;

		root = new StackPane();
		root.getChildren().add(loadPlayer());
		playlistRoot = loadPlaylist();
		searchRoot = loadSearch();

		properties.currentMediaProperty().addListener((p,o,n) -> updateAddToLibraryMenu());
		index.addMediaIndexListener(new MediaIndexListener() {
			@Override
			public void onRemoved(MediaIndexEvent e) {
				updateAddToLibraryMenu();
			}
			@Override
			public void onAdded(MediaIndexEvent e) {
				updateAddToLibraryMenu();
			}
		});

		FileDropOverlay overlay = new FileDropOverlay(root);
		overlay.setActionGenerator(files -> generateDropButtons(files));

		stage.setScene(scene = new Scene(root));
        settings = new AppSettings(properties);
        settings.getStylableStages().add(stage);

		stage.setTitle("Cyclops");
		stage.getIcons().add(FXIcons.get("Play2.png", 32).getImage());

		stage.setOnHidden(e -> {
			quit();
		});
	}

	private BorderPane loadPlayer() throws IOException {
		FXMLLoader loader = new FXMLLoader(getClass().getResource("mp3player.fxml"));
		loader.setController(this);
		BorderPane playerRoot = loader.load();

		control = new PlayerControl();
		control.durationProperty().bind(properties.durationProperty());
		control.positionProperty().bindBidirectional(properties.positionProperty());
		control.playingProperty().bindBidirectional(properties.playingProperty());
		control.mediaSelectedProperty().bind(properties.mediaSelectedProperty());
		control.playlistAvailableProperty().bind(properties.playlistAvailableProperty());
		control.shuffledProperty().bindBidirectional(properties.shuffledProperty());
		control.loopProperty().bindBidirectional(properties.loopProperty());
		control.setOnNext(e -> properties.getStatus().next());
		control.setOnPrevious(e -> properties.getStatus().previous());
		control.setOnStop(e -> properties.stop());
		control.setOnShowPlaylist(e -> showPlaylist());
		control.setOnSearch(e -> showSearch());
		playerRoot.setCenter(control);
		return playerRoot;
	}

	private BorderPane loadPlaylist() throws IOException {
		FXMLLoader loader = new FXMLLoader(getClass().getResource("playlist.fxml"));
		loader.setController(this);
		BorderPane playlistRoot = loader.load();
		playlistRoot.setBackground(new Background(new BackgroundFill(new Color(0,0,0,0.5), CornerRadii.EMPTY, Insets.EMPTY)));
		return playlistRoot;
	}

	private BorderPane loadSearch() throws IOException {
		FXMLLoader loader = new FXMLLoader(getClass().getResource("search.fxml"));
		loader.setController(this);
		BorderPane searchRoot = loader.load();
		searchRoot.setBackground(new Background(new BackgroundFill(new Color(0,0,0,0.5), CornerRadii.EMPTY, Insets.EMPTY)));
		return searchRoot;
	}


	@Override
	public void initialize(URL location, ResourceBundle resources) {
		if(playlist == null) {
			// Initialize UI
			settingsMenu.setText(null);
			settingsMenu.setGraphic(FXIcons.get("Settings.png", 24));
			currentSongMenu.setGraphic(FXIcons.get("Media.png", 24));
			currentSongMenu.textProperty().bind(properties.titleProperty());
			currentSongMenu.disableProperty().bind(properties.mediaSelectedProperty().not());
			volume.valueProperty().bindBidirectional(properties.gainProperty());
			speakerSelection.setItems(properties.getSpeakers());
			properties.getSpeaker().ifPresent(speaker -> speakerSelection.getSelectionModel().select(speaker));
			speakerSelection.getSelectionModel().selectedItemProperty().addListener((p,o,n) -> {
				if(n != null) properties.setSpeaker(Optional.of(n));
			});
			properties.speakerProperty().addListener((p,o,n) -> {
				speakerSelection.getSelectionModel().select(n.orElse(null));
			});
		}
		else if(searchField == null) {
			// Initialize playlist view
			playlist.setItems(properties.getPlaylist());
			playlist.addEventFilter(KeyEvent.KEY_PRESSED, new TabAndEnterHandler(playlist));
			removeOthersButton.disableProperty().bind(properties.playlistAvailableProperty().not());
			playlist.getSelectionModel().selectedItemProperty().addListener((p,o,n) -> {
				if(n != null) properties.setCurrentMedia(Optional.of(n.getIdentifier()));
			});
			properties.currentMediaProperty().addListener((p,o,n) -> {
				playlist.getSelectionModel().select(properties.getCurrentMedia().flatMap(m -> index.getIndex().getInfo(m)).orElse(null));
			});
			playlist.setOnMouseReleased(e -> {
//				if(e.getButton() == MouseButton.PRIMARY) {
//					Platform.runLater(() -> closePlaylist());
//				}
			});
			playlist.setCellFactory(list -> new MediaCell());
		}
		else {
			// Initialize search view
			searchField.textProperty().addListener((p,o,n) -> {
				if(n.isEmpty()) {
					searchResult.setItems(index.getRecentlyUsed().getItems());
				} else {
					searchResult.setItems(index.startSearch(n).getItems());
				}
				if(!searchResult.getItems().isEmpty()) searchResult.getSelectionModel().select(0);
				searchResult.getItems().addListener((ListChangeListener<MediaInfo>) change -> {
					if(!searchResult.getItems().isEmpty()) searchResult.getSelectionModel().select(0);
				});
			});
			searchResult.setItems(index.getRecentlyUsed().getItems());
			searchResult.setCellFactory(list -> new MediaCell());
			searchResult.setOnKeyPressed(e -> {
				if(e.getCode() == KeyCode.ENTER) {
					MediaInfo m = searchResult.getSelectionModel().getSelectedItem();
					if(m != null) {
						playFromLibrary(m.getIdentifier(), e.isControlDown());
					}
					Platform.runLater(() -> closeSearch());
				}
			});
			searchField.setOnKeyPressed(e -> {
				if(e.getCode() == KeyCode.ENTER) {
					MediaInfo m = searchResult.getSelectionModel().getSelectedItem();
					if(m != null) {
						playFromLibrary(m.getIdentifier(), e.isControlDown());
					}
					e.consume();
					Platform.runLater(() -> closeSearch());
				} else if(e.getCode() == KeyCode.DOWN) {
					int next = searchResult.getSelectionModel().getSelectedIndex()+1;
					if(searchResult.getItems().size() > next) searchResult.getSelectionModel().select(next);
					e.consume();
				} else if(e.getCode() == KeyCode.UP){
					int prev = searchResult.getSelectionModel().getSelectedIndex() - 1;
					if(prev >= 0) searchResult.getSelectionModel().select(prev);
					e.consume();
				}
			});
			searchResult.setOnMouseReleased(e -> {
				if(e.getButton() == MouseButton.PRIMARY) {
					MediaInfo m = searchResult.getSelectionModel().getSelectedItem();
					if(m != null && (!properties.getCurrentMedia().isPresent() || !m.equals(properties.getCurrentMedia()))) {
						playFromLibrary(m.getIdentifier(), e.isControlDown());
					}
					Platform.runLater(() -> closeSearch());
				}
			});
		}
	}

	public PlayerStatusWrapper getStatusWrapper() {
		return properties;
	}

	private List<ToggleButton> generateDropButtons(List<File> files) {
		List<ToggleButton> result = new ArrayList<>(3);

		List<File> audioFiles = AudioFiles.trim(AudioFiles.unfold(files));
		boolean cold = status.getPlaylist().isEmpty();

		// Play / New Playlist
		if(!audioFiles.isEmpty()) {
			ToggleButton play = new ToggleButton("Play", FXIcons.get("Play2.png", 32));
			play.setOnAction(e -> play(audioFiles, files.get(0)));
			result.add(play);
		}

		// Add to Playlist
		if(!cold && !audioFiles.isEmpty()) {
			ToggleButton append = new ToggleButton("Add to playlist", FXIcons.get("Append.png", 32));
			append.setOnAction(e -> {
				List<RemoteFile> remoteFiles = audioFiles.stream().map(file -> status.getVdp().mountFile(file)).collect(Collectors.toList());
				Identifier mediaID = status.getPlaylist().addAll(remoteFiles, 0, status.getTarget().isShuffled(), status.getPlayback().getCurrentMedia());
				if(!status.getPlayback().getCurrentMedia().isPresent()) {
					status.getTarget().setTargetMedia(mediaID, true);
				}
			});
			result.add(append);
		}

		// Play Folder
		if(files.size() == 1) {
			File file = files.get(0);
			List<File> allAudioFiles = AudioFiles.allAudioFilesIn(files.get(0).getParentFile());
			if(allAudioFiles.size() > 1) {
				ToggleButton playFolder = new ToggleButton("Play folder", FXIcons.get("PlayFolder.png", 32));
				playFolder.setOnAction(e -> play(allAudioFiles, AudioFiles.isAudioFile(file) ? file : allAudioFiles.get(0)));
				result.add(playFolder);
			}
		}

		return result;
	}

	public void play(List<File> localFiles, File startFile) {
		int startIndex = localFiles.indexOf(startFile);
		List<RemoteFile> remoteFiles = localFiles.stream().map(file -> status.getVdp().mountFile(file)).collect(Collectors.toList());
		for(RemoteFile file : remoteFiles) {
			if(!properties.getIndex().isIndexed(file)) {
				properties.getIndex().getOrAdd(file);
			}
		}
		Identifier mediaID = status.getPlaylist().setAll(remoteFiles, startIndex, status.getTarget().isShuffled(), false);
		status.getTarget().setTargetMedia(mediaID, true);
	}

	private void fadeIn(Node node, Node focus) {
		if(root.getChildren().contains(node)) {
			fadeOut(node);
			return;
		}
		if(currentOverlay != null) {
			fadeOut(currentOverlay);
		}
		currentOverlay = node;
		root.getChildren().add(node);

		Duration time = new Duration(200);
		TranslateTransition in = new TranslateTransition(time, node);
		in.setFromY(-20);
		in.setToY(0);
		in.play();
		FadeTransition fade = new FadeTransition(time, node);
		fade.setFromValue(0);
		fade.setToValue(1);
		fade.play();

		focus.requestFocus();
	}

	private void fadeOut(Node node) {
		Duration time = new Duration(200);

		TranslateTransition in = new TranslateTransition(time, node);
		in.setFromY(0);
		in.setToY(-10);
		in.play();

    	FadeTransition fade = new FadeTransition(time, node);
		fade.setFromValue(1);
		fade.setToValue(0);
		fade.play();
		fade.setOnFinished(e -> root.getChildren().remove(node));

		currentOverlay = null;
	}

	public void showPlaylist() {
		fadeIn(playlistRoot, playlist);
	}
    @FXML
    public void closePlaylist() {
    	fadeOut(playlistRoot);
    }

    @FXML
	public void showSearch() {
		fadeIn(searchRoot, searchField);
	}

	@FXML
	public void closeSearch() {
		fadeOut(searchRoot);
	}

	private void updateAddToLibraryMenu() {
		addToLibraryMenu.getItems().clear();

		Optional<RemoteFile> op = properties.getCurrentMedia().flatMap(id -> id.lookup(status.getVdp()));
		if(op.isPresent() && op.get().localFile() != null) {
			File file = op.get().localFile().getAbsoluteFile();
			while(op.isPresent() && index.getIndex().isIndexed(op.get())) {
				op = op.get().getParentFile();
				file = file.getParentFile();
			}
			if(file != null) {
				do {
					File ffile = file;
					MenuItem item = new MenuItem(file.getName().isEmpty() ? file.getAbsolutePath() : file.getName());
					item.setGraphic(FXIcons.get(file.isDirectory() ? "PlayFolder.png" : "Media.png", 28) );
					item.setOnAction(e -> index.getIndex().addLocalRoot(ffile));
					addToLibraryMenu.getItems().add(item);
					file = file.getParentFile();
				} while(file != null);
			}
		}

		if(addToLibraryMenu.getItems().isEmpty()) {
			addToLibraryMenu.getItems().setAll(Arrays.asList(cannotAddToLibraryItem));
		}
	}


	private void playFromLibrary(Identifier media, boolean append) {
		Optional<RemoteFile> opFile = media.lookup(status.getVdp());
		opFile.ifPresent(file -> {
			List<RemoteFile> remoteFiles = Arrays.asList(file);
			if(!append) {
				Identifier mediaID = status.getPlaylist().setAll(remoteFiles, 0, status.getTarget().isShuffled(), true);
				status.getTarget().setTargetMedia(mediaID, true);
			} else {
				Identifier mediaID = status.getPlaylist().addAll(remoteFiles, 0, status.getTarget().isShuffled(), status.getPlayback().getCurrentMedia());
				if(!status.getPlayback().getCurrentMedia().isPresent()) {
					status.getTarget().setTargetMedia(mediaID, true);
				}
			}
		});
	}


	static class MediaCell extends ListCell<MediaInfo>
	{
		@Override
		protected void updateItem(MediaInfo item, boolean empty) {
			super.updateItem(item, empty);
			if(item != null) {
				setText(item.getDisplayTitle());
			} else setText(null);
		}
	}

    public void show() {
		stage.show();
		System.out.println(stage.getWidth()+" x "+stage.getHeight());

		// default values, apply for bundled application
    	stage.setWidth(314);
    	stage.setHeight(402);
    }

    @FXML
    public void quit() {
    	System.exit(0);
    }

    @FXML
    public void clearPlaylist() {
    	status.getTarget().setTargetMedia(Optional.empty(), false);
    	status.getPlaylist().clear();
    	closePlaylist();
    }

    @FXML
    public void clearOthers() {
    	List<Identifier> newList = new ArrayList<>();
    	properties.getStatus().getPlayback().getCurrentMedia().ifPresent(m -> newList.add(m));
    	properties.getStatus().getPlaylist().setAll(newList);
    }

    @FXML
    public void displayInfo() {
    	Alert info = new Alert(AlertType.INFORMATION);
    	info.setTitle("Info");
    	info.setHeaderText("Cyclops");
    	info.setContentText("Version 0.2\nAuthor: Philipp Holl\nMay 2020");
    	info.initOwner(stage);
    	info.initModality(Modality.NONE);
    	info.show();
    }

    @FXML
    public void openFileLocation() {
    	status.getPlayback().getCurrentMedia().flatMap(id -> id.lookup(status.getVdp())).ifPresent(file -> {
    		if(file.localFile() != null) {
    			try {
					Desktop.getDesktop().browse(file.localFile().getParentFile().toURI());
				} catch (NoSuchElementException | IOException e) {
					e.printStackTrace();
					new Alert(AlertType.ERROR, "Could not open location: "+file.localFile(), ButtonType.OK).show();
				}
    		} else {
    			new Alert(AlertType.INFORMATION, "The file is not located on this systemcontrol.", ButtonType.OK).show();
    		}
    	});
    }

    @FXML
    public void removeCurrentFromPlaylist() {
    	Optional<Identifier> current = status.getPlayback().getCurrentMedia();
    	if(!current.isPresent()) return;

    	boolean hasNext = status.getNext() != current;
    	if(hasNext) {
    		status.next();
    	}
    	status.getPlaylist().remove(current.get());
    	if(!hasNext) status.next();
    }



	class TabAndEnterHandler implements EventHandler<KeyEvent> {
		private Node node;

		public TabAndEnterHandler(Node node) {
			this.node = node;
		}

		public void handle(KeyEvent event) {
			Parent parent = node.getParent();
			if(event.getCode() == KeyCode.ENTER || event.getCode() == KeyCode.ESCAPE) {
				parent.fireEvent(event.copyFor(parent, parent));
				event.consume();
			}
		}
	}


	@FXML
	public void showFileInfo() throws IOException {
		if(!status.getPlayback().getCurrentMedia().isPresent()) {
			return;
		}

		FXMLLoader loader = new FXMLLoader(getClass().getResource("fileinfo.fxml"));
		loader.setController(new Initializable() {
		    @FXML
            private Tab encodingTab, playbackTab;
			@FXML
			private Label titleLabel, durationLabel, encodingLabel;
			@FXML
			private Hyperlink pathLink;
			@FXML
			private TableView<Map.Entry<String, Object>> propertiesTable;
            @FXML
            private TableColumn<Map.Entry<String, Object>, String> propertyColumn;
			@FXML
            private TableColumn<Map.Entry<String, Object>, Object> valueColumn;
			@FXML
            private Label eEncoding, eChannels, eSampleRate, eSampleSize, eFrameSize, eFrameRate, eEndianness, eProperties;
            @FXML
            private Label dEncoding, dChannels, dSampleRate, dSampleSize, dFrameSize, dFrameRate, dEndianness, dProperties, playbackEngine;

			@Override
			public void initialize(URL location, ResourceBundle resources) {
				if(engine.currentPlayer() != null) {
					Player player = engine.currentPlayer();
					try {
						player.loadMediaFormat();
					} catch (IOException | UnsupportedMediaFormatException e) {
						e.printStackTrace();
						Alert alert = new Alert(AlertType.ERROR);
						alert.setTitle("File error");
						alert.setHeaderText("Failed to retrieve media information.");
						alert.setContentText(e.getMessage());
						alert.showAndWait();
						return;
					}
					MediaFile file = player.getMediaFile();
					MediaFormat format = player.getMediaFormat();
					audio.MediaInfo info = player.getMediaInfo();

					titleLabel.setText(info.getTitle() != null ? info.getTitle() : file.getFileName());
					pathLink.setText(file.getFile().getAbsolutePath());
					durationLabel.setText("Duration: " + info.getDuration() + " seconds / " + format.getFrameLength() + " frames");
					encodingLabel.setText("File type: " + format.getType().getName() + ", size: " + file.getFileSize() / 1024 / 1024 + " MB (" + file.getFileSize() + " bytes)");
					propertiesTable.getItems().addAll(FXCollections.observableArrayList(format.getProperties().entrySet()));

                    AudioDataFormat ef = player.getEncodedFormat();
                    eEncoding.setText("Encoding: " + ef.getEncodingName());
                    eChannels.setText("Channels: " + (ef.getChannels() == 2 ? "Stereo" : (ef.getChannels() == 1 ? "Mono" : ef.getChannels())));
                    eSampleRate.setText("Sample rate: " + ef.getSampleRate() + " Hz");
                    eSampleSize.setText("Sample size: " + (ef.getSampleSizeInBits() > 0 ? ef.getSampleSizeInBits() + " bits" : "variable"));
                    eFrameSize.setText("Frame size: " + (ef.getFrameSize() > 0 ? ef.getFrameSize() + " bytes" : "variable"));
                    eFrameRate.setText("Frame rate: " + ef.getFrameRate() + " Hz");
                    eEndianness.setText("Endianness: " + (ef.isBigEndian() ? "big endian" : "little endian"));
                    eProperties.setText((ef.getProperties().isEmpty() ? "" : ef.getProperties().toString()));

                    playbackEngine.setText("Playback engine: " + format.getAudioEngineName());
                    AudioDataFormat df = player.getDecodedFormat();
                    dEncoding.setText("Encoding: " + df.getEncodingName());
                    dChannels.setText("Channels: " + (df.getChannels() == 2 ? "Stereo" : (df.getChannels() == 1 ? "Mono" : df.getChannels())));
                    dSampleRate.setText("Sample rate: " + df.getSampleRate() + " Hz");
                    dSampleSize.setText("Sample size: " + (df.getSampleSizeInBits() > 0 ? df.getSampleSizeInBits() + " bits" : "variable"));
                    dFrameSize.setText("Frame size: " + (df.getFrameSize() > 0 ? df.getFrameSize() + " bytes" : "variable"));
                    dFrameRate.setText("Frame rate: " + df.getFrameRate() + " Hz");
                    dEndianness.setText("Endianness: " + (df.isBigEndian() ? "big endian" : "little endian"));
                    dProperties.setText((df.getProperties().isEmpty() ? "" : df.getProperties().toString()));
				} else {
					status.getPlayback().getCurrentMedia().ifPresent(media -> {
						titleLabel.setText(media.getFileName());
						pathLink.setText(media.getPath());
						durationLabel.setText("Media details are unavailable because file is not stored locally.");
					});
					encodingTab.setDisable(true);
					playbackTab.setDisable(true);
				}
				propertyColumn.setCellValueFactory(entry -> new SimpleStringProperty(entry.getValue().getKey()));
				valueColumn.setCellValueFactory(entry -> new SimpleObjectProperty<>(entry.getValue().getValue()));
			}
		});

        BorderPane playerRoot = loader.load();
        Stage stage = new Stage();
        stage.setTitle("Media Info");
        stage.setScene(new Scene(playerRoot));
        settings.getStylableStages().add(stage);
        stage.show();
	}

	@FXML
    private void showSettings() {
        settings.getStage().show();
    }
}
