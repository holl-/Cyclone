package player.fx.app;

import appinstance.ApplicationParameters;
import appinstance.InstanceManager;
import audio.AudioEngineException;
import distributed.DistributedPlatform;
import javafx.application.Application;
import javafx.stage.Stage;
import mediacommand.CombinationManager;
import mediacommand.MediaCommand;
import mediacommand.MediaCommandManager;
import player.model.CyclonePlayer;
import player.model.MediaLibrary;
import player.model.PlaybackEngine;
import player.model.data.SpeakerList;
import systemcontrol.LocalMachine;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class Launcher extends Application {
	private PlayerWindow window;

	@Override
	public void start(Stage primaryStage) throws Exception
	{
		ApplicationParameters appParams = new ApplicationParameters("Cyclone", getParameters());
		InstanceManager im = new InstanceManager(appParams, params -> play(params));
		Optional<ApplicationParameters> mainAppParams = im.registerIfFirst();

		if(!mainAppParams.isPresent()) {
			setup(primaryStage);
			play(appParams);
		}
		else {
			// Parameters have been passed to the main instance
			System.exit(0);
		}
	}

	private void setup(Stage primaryStage) throws IOException, AudioEngineException {
		CyclonePlayer player = new CyclonePlayer(new MediaLibrary());
		PlaybackEngine engine = PlaybackEngine.initializeAudioEngine(player.getPlayerTarget(), player.getPlaybackStatus(), null);
//		player.getDevicesData().setSpeakers(engine.getSpeakers());

		window = new PlayerWindow(primaryStage, player, engine);
		window.show();
		addControl(window.getStatusWrapper());

		DistributedPlatform platform = new DistributedPlatform();
		platform.putData(player.getDistributedObjects());
		platform.getData(SpeakerList.class).setSpeakers(engine.getSpeakers());
	}

	private void play(ApplicationParameters parameters) {
		List<File> files = parameters.getUnnamed().stream().map(path -> new File(path)).filter(file -> file.exists()).collect(Collectors.toList());
		if(!files.isEmpty()) {
			window.play(files, files.get(0));
		}
	}

	public static void main(String[] args) {
		launch(args);
	}


	public static void addControl(CyclonePlayer player) {
		if(MediaCommandManager.isSupported()) {
        	MediaCommandManager manager = MediaCommandManager.getInstance();
        	CombinationManager cm = new CombinationManager();
        	cm.register(manager);

        	cm.addCombination(new MediaCommand[]{ MediaCommand.PLAY_PAUSE }, c -> {
        		player.setPlaying(!player.isPlaying());
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
        		player.setPlaying(!player.isPlaying());
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
