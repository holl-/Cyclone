package player.fx.app;

import appinstance.ApplicationParameters;
import appinstance.InstanceManager;
import audio.AudioEngineException;
import cloud.Cloud;
import cloud.Peer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import mediacommand.CombinationManager;
import mediacommand.MediaCommand;
import mediacommand.MediaCommandManager;
import player.model.CycloneConfig;
import player.model.PlaylistPlayer;
import player.model.playback.PlaybackEngine;
import systemcontrol.LocalMachine;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static player.model.ConfigKt.getConfigFile;

public class Launcher extends Application {
	private PlayerWindow window;
	private CycloneConfig config;

	@Override
	public void start(Stage primaryStage) throws Exception
	{
		ApplicationParameters appParams = new ApplicationParameters("Cyclone", getParameters());

		config = CycloneConfig.Companion.getGlobal();
		boolean singleInstance = config.getSingleInstance().get();
		if(singleInstance) {
			InstanceManager im = new InstanceManager(appParams, params -> play(params));
			Optional<ApplicationParameters> mainAppParams = im.registerIfFirst();

			if (mainAppParams.isPresent()) {
				System.exit(0);  // Parameters have been passed to the main instance
				return;
			}
		}

		try {
			setup(primaryStage);
			play(appParams);
		} catch(Throwable t) {
			t.printStackTrace();
		}
	}

	private void setup(Stage primaryStage) throws IOException, AudioEngineException {
		String computerName = config.getComputerName().get();
		if (computerName != null) Peer.getLocal().setName(computerName);

		Cloud cloud1 = new Cloud();
		File prevStatus = getConfigFile("status.cld");
		if (prevStatus.exists()) {
			try {
				cloud1.read(prevStatus, true);
			}catch (Exception exc) {
				exc.printStackTrace();
			}
		}

		PlaybackEngine engine = new PlaybackEngine(cloud1, config);

		PlaylistPlayer player = new PlaylistPlayer(cloud1, config);
		window = new PlayerWindow(primaryStage, player, engine, config);
		window.show();
		addControl(window.getPlayer());

		if (config.getConnectOnStartup().get()) {
			cloud1.connect(config.getMulticastAddress().get(), config.getMulticastPort().get(), true, 1000);
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

	public static void main(String[] args)
	{
		try {
			Files.write(getConfigFile("launch_args.txt").toPath(), Arrays.asList(args));
		} catch (IOException e) {
			e.printStackTrace();
		}
		launch(fixArgs(args));
	}

	/** If file is split at spaces, recombines the parts into a single file. */
	private static String[] fixArgs(String[] args) {
		if (args.length <= 1) return args;
		File singleFile = new File(String.join(" ", args));
		if(singleFile.exists()) return new String[]{singleFile.getPath()};
		else return args;
	}


	public static void addControl(PlaylistPlayer player) {
		if(MediaCommandManager.isSupported()) {
        	MediaCommandManager manager = MediaCommandManager.getInstance();
        	CombinationManager cm = new CombinationManager();
        	cm.register(manager);

        	cm.addCombination(new MediaCommand[]{ MediaCommand.PLAY_PAUSE }, c -> {
        		Platform.runLater(() -> player.getPlayingProperty().set(!player.getPlayingProperty().get()));
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
