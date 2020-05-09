package player.fx.app;

import appinstance.ApplicationParameters;
import appinstance.InstanceManager;
import audio.AudioEngineException;
import audio.javasound.JavaSoundEngine;
import cloud.Cloud;
import cloud.Peer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import mediacommand.CombinationManager;
import mediacommand.MediaCommand;
import mediacommand.MediaCommandManager;
import player.fx.debug.CloudViewer;
import player.fx.debug.PlaybackViewer;
import player.fx.debug.TaskViewer;
import player.model.CycloneConfig;
import player.model.PlaylistPlayer;
import player.model.playback.PlaybackEngine;
import systemcontrol.LocalMachine;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class Launcher extends Application {
	private PlayerWindow window;
	private CycloneConfig config;

	@Override
	public void start(Stage primaryStage) throws Exception
	{
		ApplicationParameters appParams = new ApplicationParameters("Cyclone", getParameters());

		config = CycloneConfig.Companion.getGlobal();
		boolean singleInstance = Boolean.parseBoolean(config.getString("singleInstance", "true"));
		if(singleInstance) {
			InstanceManager im = new InstanceManager(appParams, params -> play(params));
			Optional<ApplicationParameters> mainAppParams = im.registerIfFirst();

			if (mainAppParams.isPresent()) {
				System.exit(0);  // Parameters have been passed to the main instance
				return;
			}
		}

		setup(primaryStage);
		play(appParams);
	}

	private void setup(Stage primaryStage) throws IOException, AudioEngineException {
		try {
			Cloud cloud = new Cloud();
			PlaybackEngine engine = new PlaybackEngine(cloud, new JavaSoundEngine(), config);

			PlaylistPlayer player = new PlaylistPlayer(cloud, config);
			window = new PlayerWindow(primaryStage, player, engine, config);
			window.show();
			addControl(window.getStatusWrapper());

//			TaskViewer viewer = new TaskViewer(cloud, new Stage());
//			viewer.getStage().show();
//			PlaybackViewer pbv = new PlaybackViewer(engine);
//			pbv.getStage().show();
//			CloudViewer cv = new CloudViewer(cloud);
//			cv.getStage().show();

			Cloud cloud2 = new Cloud();
			cloud2.setPeer$Cyclone(new Peer(true, "peer2", "localhost", "2"));
			PlaylistPlayer player2 = new PlaylistPlayer(cloud2, config);
			PlayerWindow window2 = new PlayerWindow(new Stage(), player2, engine, config);
			window2.show();

			cloud.connect("225.4.5.6", 5324, 5325);
			new Thread(() -> {
				try {
					Thread.sleep(1000);
					cloud2.connect("225.4.5.6", 5324, 5335);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}).start();
		} catch(Throwable t) {
			t.printStackTrace();
		}
	}

	private void play(ApplicationParameters parameters) {
		List<File> files = parameters.getUnnamed().stream().map(path -> new File(path)).filter(file -> file.exists()).collect(Collectors.toList());
		if(!files.isEmpty()) {
			Platform.runLater(() -> {
				try {
					window.play(files, files.get(0));
				} catch(Throwable t) {
					t.printStackTrace();
				}
			});
		}
	}

	public static void main(String[] args) {
		launch(args);
	}


	public static void addControl(PlaylistPlayer player) {
		if(MediaCommandManager.isSupported()) {
        	MediaCommandManager manager = MediaCommandManager.getInstance();
        	CombinationManager cm = new CombinationManager();
        	cm.register(manager);

        	cm.addCombination(new MediaCommand[]{ MediaCommand.PLAY_PAUSE }, c -> {
        		player.getPlayingProperty().set(!player.getPlayingProperty().get());
        	});
        	cm.addCombination(new MediaCommand[]{ MediaCommand.STOP }, c -> player.stop() );
        	cm.addCombination(new MediaCommand[]{ MediaCommand.NEXT }, c -> player.next() );
        	cm.addCombination(new MediaCommand[]{ MediaCommand.PREVIOUS }, c -> player.previous() );

        	MediaCommand[] playCombination = new MediaCommand[]{ MediaCommand.VOLUME_UP, MediaCommand.VOLUME_DOWN};
        	MediaCommand[] monitorOffCombination = new MediaCommand[]{ MediaCommand.VOLUME_DOWN, MediaCommand.VOLUME_UP};
        	MediaCommand[] nextCombination = new MediaCommand[]{ MediaCommand.VOLUME_UP, MediaCommand.VOLUME_UP, MediaCommand.VOLUME_DOWN, MediaCommand.VOLUME_DOWN};
        	MediaCommand[] previousCombination = new MediaCommand[]{ MediaCommand.VOLUME_DOWN, MediaCommand.VOLUME_DOWN, MediaCommand.VOLUME_UP, MediaCommand.VOLUME_UP};
        	MediaCommand[] deleteCombination = new MediaCommand[]{ MediaCommand.VOLUME_DOWN, MediaCommand.MUTE, MediaCommand.MUTE, MediaCommand.VOLUME_UP };

        	cm.addCombination(playCombination, c -> {
				player.getPlayingProperty().set(!player.getPlayingProperty().get());
        	});
        	cm.addCombination(monitorOffCombination, c -> {
        		LocalMachine machine = LocalMachine.getLocalMachine();
        		if(machine != null) machine.turnOffMonitors();
        	});
        	cm.addCombination(nextCombination, c -> player.next());
        	cm.addCombination(previousCombination, c -> player.previous());
        	cm.addCombination(deleteCombination, c -> System.out.println("Delete not implemented yet"));
        }
	}

}
