package player.fx.app;

import audio.*;
import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
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
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import player.model.AudioFiles;
import player.model.CycloneConfig;
import player.fx.FileDropOverlay;
import player.fx.PlayerControl;
import player.fx.icons.FXIcons;
import player.model.CyclonePlayer;
import player.model.MediaLibrary;
import player.model.PlaybackEngine;
import player.model.data.Speaker;
import distributed.DFile;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class PlayerWindow implements Initializable {
	private Stage stage;
	private StackPane root;
	private Node currentOverlay;

	// Default
	@FXML private Menu currentSongMenu, settingsMenu, addToLibraryMenu;
	@FXML private MenuItem cannotAddToLibraryItem;
	@FXML private MenuBar menuBar;
	@FXML private Slider volume;
	@FXML private ComboBox<Speaker> speakerSelection;
	// Playlist
	private Node playlistRoot, playlistListView;
	// Search
	private Node searchRoot, searchFocus;

	private CyclonePlayer player;
	private MediaLibrary library;
	private PlaybackEngine engine; // can be null
	private AppSettings settings;


	PlayerWindow(Stage stage, CyclonePlayer player, PlaybackEngine engine, CycloneConfig config) throws IOException {
		this.stage = stage;
		this.player = player;
		this.engine = engine;
		library = player.getLibrary();

		root = new StackPane();
		root.getChildren().add(loadPlayer());
		playlistRoot = loadPlaylist();
		searchRoot = loadSearch();

		this.player.currentMediaProperty().addListener((p, o, n) -> updateAddToLibraryMenu());
//		index.addMediaIndexListener(new MediaIndexListener() {
//			@Override
//			public void onRemoved(MediaIndexEvent e) {
//				updateAddToLibraryMenu();
//			}
//			@Override
//			public void onAdded(MediaIndexEvent e) {
//				updateAddToLibraryMenu();
//			}
//		});

		FileDropOverlay overlay = new FileDropOverlay(root);
		overlay.setActionGenerator(files -> generateDropButtons(files));

		Scene scene;
		stage.setScene(scene = new Scene(root));
        settings = new AppSettings(config, this.player);
        settings.getStylableStages().add(stage);

		stage.setTitle("Cyclone");
		stage.getIcons().add(FXIcons.get("Play2.png", 32).getImage());

		stage.setOnHidden(e -> {
			quit();
		});
	}

	private BorderPane loadPlayer() throws IOException {
		FXMLLoader loader = new FXMLLoader(getClass().getResource("mp3player.fxml"));
		loader.setController(this);
		BorderPane playerRoot = loader.load();

		PlayerControl control = new PlayerControl();
		control.durationProperty().bind(player.durationProperty());
		control.positionProperty().bindBidirectional(player.positionProperty());
		control.playingProperty().bindBidirectional(player.playingProperty());
		control.mediaSelectedProperty().bind(player.mediaSelectedProperty());
		control.playlistAvailableProperty().bind(player.playlistAvailableProperty());
		control.shuffledProperty().bindBidirectional(player.shuffledProperty());
		control.loopProperty().bindBidirectional(player.loopProperty());
		control.setOnNext(e -> player.next());
		control.setOnPrevious(e -> player.previous());
		control.setOnStop(e -> player.stop());
		control.setOnShowPlaylist(e -> showPlaylist());
		control.setOnSearch(e -> showSearch());
		playerRoot.setCenter(control);

//		control.setOnMouseDragged(e -> e.);

		return playerRoot;
	}

	private BorderPane loadPlaylist() throws IOException {
		FXMLLoader loader = new FXMLLoader(getClass().getResource("playlist.fxml"));
		loader.setController(new Initializable() {
			@FXML private Button removeOthersButton;
			@FXML private ListView<DFile> playlist;

			@Override
			public void initialize(URL location, ResourceBundle resources) {
				playlist.setItems(player.getPlaylist());
				playlist.addEventFilter(KeyEvent.KEY_PRESSED, new TabAndEnterHandler(playlist));
				removeOthersButton.disableProperty().bind(player.playlistAvailableProperty().not());
				playlist.getSelectionModel().selectedItemProperty().addListener((p,o,n) -> {
					if(n != null) player.setCurrentMedia(Optional.of(n));
				});
				player.currentMediaProperty().addListener((p, o, n) -> {
					playlist.getSelectionModel().select(player.getCurrentMedia().orElse(null));
				});
				playlist.setCellFactory(list -> new MediaCell());
				playlistListView = playlist;
			}

			@FXML
			public void closePlaylist() {
				fadeOut(playlistRoot);
			}

			@FXML
			public void clearPlaylist() {
				player.setCurrentMedia(Optional.empty());
				player.getPlaylist().clear();
				closePlaylist();
			}

			@FXML
			public void clearOthers() {
				List<DFile> newList = new ArrayList<>();
				player.getCurrentMedia().ifPresent(m -> newList.add(m));
				player.setPlaylist(newList, 0, false);
			}
		});
		BorderPane playlistRoot = loader.load();
		playlistRoot.setBackground(new Background(new BackgroundFill(new Color(0,0,0,0.5), CornerRadii.EMPTY, Insets.EMPTY)));
		return playlistRoot;
	}

	private BorderPane loadSearch() throws IOException {
		FXMLLoader loader = new FXMLLoader(getClass().getResource("search.fxml"));
		loader.setController(new Initializable() {
			@FXML private ListView<DFile> searchResult;
			@FXML private TextField searchField;

			@Override
			public void initialize(URL location, ResourceBundle resources) {
				searchField.textProperty().addListener((p,o,n) -> {
					if(n.isEmpty()) {
						searchResult.setItems(library.getRecentlyUsed());
					} else {
						searchResult.setItems(library.startSearch(n));
					}
					if(!searchResult.getItems().isEmpty()) searchResult.getSelectionModel().select(0);
					searchResult.getItems().addListener((ListChangeListener<DFile>) change -> {
						if(!searchResult.getItems().isEmpty()) searchResult.getSelectionModel().select(0);
					});
				});
				searchResult.setItems(library.getRecentlyUsed());
				searchResult.setCellFactory(list -> new MediaCell());
				searchResult.setOnKeyPressed(e -> { if(e.getCode() == KeyCode.ENTER) playSelected(e.isControlDown()); });
				searchField.setOnKeyPressed(e -> {
					if(e.getCode() == KeyCode.ENTER) {
						playSelected(e.isControlDown());
						e.consume();
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
				searchResult.setOnMouseReleased(e -> { if(e.getButton() == MouseButton.PRIMARY) playSelected(e.isControlDown()); });
				searchFocus = searchField;
			}

			@FXML
			public void closeSearch() {
				fadeOut(searchRoot);
			}

			private void playSelected(boolean append) {
				DFile m = searchResult.getSelectionModel().getSelectedItem();
				if(m != null) {
					playFromLibrary(m, append);
				}
				Platform.runLater(() -> closeSearch());
			}
		});
		BorderPane searchRoot = loader.load();
		searchRoot.setBackground(new Background(new BackgroundFill(new Color(0,0,0,0.5), CornerRadii.EMPTY, Insets.EMPTY)));
		return searchRoot;
	}


	@Override
	public void initialize(URL location, ResourceBundle resources) {
		settingsMenu.setText(null);
		settingsMenu.setGraphic(FXIcons.get("Settings.png", 24));
		currentSongMenu.setGraphic(FXIcons.get("Media.png", 24));
		currentSongMenu.textProperty().bind(player.titleProperty());
		currentSongMenu.disableProperty().bind(player.mediaSelectedProperty().not());
		volume.valueProperty().bindBidirectional(player.gainProperty());
		speakerSelection.setItems(player.getSpeakers());
		player.getSpeaker().ifPresent(speaker -> speakerSelection.getSelectionModel().select(speaker));
		speakerSelection.getSelectionModel().selectedItemProperty().addListener((p,o,n) -> {
			if(n != null) player.setSpeaker(Optional.of(n));
		});
		player.speakerProperty().addListener((p, o, n) -> {
			speakerSelection.getSelectionModel().select(n.orElse(null));
		});
	}

	public CyclonePlayer getStatusWrapper() {
		return player;
	}

	private List<ToggleButton> generateDropButtons(List<File> files) {
		List<ToggleButton> result = new ArrayList<>(3);

		List<File> audioFiles = AudioFiles.trim(AudioFiles.unfold(files));
		boolean cold = player.getPlaylist().isEmpty();

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
				List<DFile> remoteFiles = audioFiles.stream().map(DFile::new).collect(Collectors.toList());
				DFile mediaID = player.addToPlaylist(remoteFiles, 0);
				if(!player.getCurrentMedia().isPresent()) {
					player.setCurrentMedia(Optional.of(mediaID));
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
		List<DFile> remoteFiles = localFiles.stream().map(DFile::new).collect(Collectors.toList());
//		for(DFile file : remoteFiles) {
//			if(!player.getLibrary().isIndexed(file)) {
//				player.getLibrary().getOrAdd(file);
//			}
//		}
		DFile mediaID = player.setPlaylist(remoteFiles, startIndex, false);
		player.setCurrentMedia(Optional.of(mediaID));
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
		fadeIn(playlistRoot, playlistListView);
	}

    @FXML
	public void showSearch() {
		fadeIn(searchRoot, searchFocus);
	}

	private void updateAddToLibraryMenu() {
		addToLibraryMenu.getItems().clear();
		addToLibraryMenu.setDisable(true);

		player.getCurrentMedia().ifPresent(file -> {
			if(file.originatesHere()) {
				File localFile = new File(file.getPath()).getAbsoluteFile();
				while(localFile != null && library.isIndexed(new DFile(localFile))) {
					localFile = localFile.getParentFile();
				}
				if(localFile != null) {
					do {
						File finalFile = localFile;
						MenuItem item = new MenuItem(localFile.getName().isEmpty() ? localFile.getAbsolutePath() : localFile.getName());
						item.setGraphic(FXIcons.get(localFile.isDirectory() ? "PlayFolder.png" : "Media.png", 28) );
						item.setOnAction(e -> library.getRoots().add(new DFile(finalFile)));
						addToLibraryMenu.getItems().add(item);
						localFile = localFile.getParentFile();
					} while(localFile != null);
					addToLibraryMenu.setDisable(false);
				}
			}
		});

		if(addToLibraryMenu.getItems().isEmpty()) {
			addToLibraryMenu.getItems().setAll(Arrays.asList(cannotAddToLibraryItem));
		}
	}


	private void playFromLibrary(DFile file, boolean append) {
		List<DFile> files;
		if(file.isDirectory()) {
			List<File> allFiles = AudioFiles.unfold(Arrays.asList(new File(file.getPath())));
			files = allFiles.stream().filter(AudioFiles::isAudioFile).map(DFile::new).collect(Collectors.toList());
		}
		else files = Arrays.asList(file);
		if(!append) {
			DFile mediaID = player.setPlaylist(files, 0, false);
			player.setCurrentMedia(Optional.ofNullable(mediaID));
		} else {
			DFile mediaID = player.addToPlaylist(files, 0);
			if(!player.getCurrentMedia().isPresent()) {
				player.setCurrentMedia(Optional.ofNullable(mediaID));
			}
		}
	}


	static class MediaCell extends ListCell<DFile>
	{
		ImageView fileIcon = FXIcons.get("Play.png", 32);
		ImageView dirIcon = FXIcons.get("PlayFolder.png", 32);

		@Override
		protected void updateItem(DFile item, boolean empty) {
			super.updateItem(item, empty);
			if(item != null) {
				setText(AudioFiles.inferTitle(item.getPath()));
				setGraphic(item.isDirectory() ? dirIcon : fileIcon);
			} else {
				setText(null);
				setGraphic(null);
			}
		}
	}

    public void show() {
		stage.show();
    	stage.setWidth(314);  // default values, apply for bundled application
    	stage.setHeight(402);
    }

    @FXML
    public void quit() {
    	System.exit(0);
    }

    @FXML
    public void displayInfo() {
    	Alert info = new Alert(AlertType.INFORMATION);
    	info.setTitle("Info");
    	info.setHeaderText("Cyclone");
    	info.setContentText("Version 0.2\nAuthor: Philipp Holl\nMay 2020");
    	info.initOwner(stage);
    	info.initModality(Modality.NONE);
    	info.show();
    }

    @FXML
    public void openFileLocation() {
    	player.getCurrentMedia().ifPresent(file -> {
    		if(file.originatesHere()) {
    			try {
					Desktop.getDesktop().browse(new File(file.getPath()).getParentFile().toURI());
				} catch (NoSuchElementException | IOException e) {
					e.printStackTrace();
					new Alert(AlertType.ERROR, "Could not open location: "+file.getPath(), ButtonType.OK).show();
				}
    		} else {
    			new Alert(AlertType.INFORMATION, "The file is not located on this systemcontrol.", ButtonType.OK).show();
    		}
    	});
    }

    @FXML
    public void removeCurrentFromPlaylist() {
    	Optional<DFile> current = player.getCurrentMedia();
    	if(!current.isPresent()) return;

    	boolean hasNext = player.getNext() != current;
    	if(hasNext) {
    		player.next();
    	}
    	player.getPlaylist().remove(current.get());
    	if(!hasNext) player.next();
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
		if(!player.getCurrentMedia().isPresent()) {
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
					player.getCurrentMedia().ifPresent(media -> {
						titleLabel.setText(media.getName());
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
