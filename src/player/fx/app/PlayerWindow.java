package player.fx.app;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import com.aquafx_project.AquaFx;
import player.fx.FileDropOverlay;
import player.fx.PlayerControl;
import player.fx.icons.FXIcons;
import player.fx.playerwrapper.MediaIndexWrapper;
import player.fx.playerwrapper.PlayerStatusWrapper;
import player.model.AudioFiles;
import player.model.Identifier;
import player.model.MediaIndex;
import player.model.MediaIndexEvent;
import player.model.MediaIndexListener;
import player.model.MediaInfo;
import player.status.PlayerStatus;
import player.status.Speaker;
import vdp.RemoteFile;

import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Application;
import javafx.application.Platform;
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
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

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


	public PlayerWindow(PlayerStatus status, MediaIndex index, Stage stage) throws IOException {
		this.stage = stage;
		this.status = status;
		properties = new PlayerStatusWrapper(status, index);
		this.index = new MediaIndexWrapper(index);

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
		scene.getStylesheets().add(getClass().getResource("defaultstyle.css").toExternalForm());

		stage.setTitle("MX Player");
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


	@FXML
	public void setStyle(ActionEvent e) {
		MenuItem item = (MenuItem) e.getTarget();
		String text = item.getText();
		if(text.equals("AquaFX")) {
			AquaFx.style();
		} else {
			Application.setUserAgentStylesheet(text.toUpperCase());
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
    	info.setHeaderText("MX Player");
    	info.setContentText("MX Player pre 0.1\nAuthor: Philipp Holl\nFeb 20, 2017");
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
}
