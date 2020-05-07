package player.fx.app;

import appinstance.ApplicationParameters;
import appinstance.InstanceManager;
import audio.AudioEngineException;
import cloud.Cloud;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import mediacommand.CombinationManager;
import mediacommand.MediaCommand;
import mediacommand.MediaCommandManager;
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
			PlaylistPlayer player = new PlaylistPlayer(cloud, config);
			PlaybackEngine engine = PlaybackEngine.initializeAudioEngine(cloud, null);

			window = new PlayerWindow(primaryStage, player, engine, config);
			window.show();
			addControl(window.getStatusWrapper());

//			TaskViewer viewer = new TaskViewer(cloud, primaryStage);
//			viewer.getStage().show();

//			PlaylistPlayer player2 = new PlaylistPlayer(cloud, config);
//			PlayerWindow window2 = new PlayerWindow(new Stage(), player2, engine, config);
//			window2.show();
		} catch(Throwable t) {
			t.printStackTrace();
		}
	}

	private void play(ApplicationParameters parameters) {
//		List<File> files = parameters.getUnnamed().stream().map(path -> new File(path)).filter(file -> file.exists()).collect(Collectors.toList());
//		if(!files.isEmpty()) {
//			Platform.runLater(() -> {
//				try {
//					window.play(files, files.get(0));
//				} catch(Throwable t) {
//					t.printStackTrace();
//				}
//			});
//		}
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
