package player.fx.app;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import appinstance.ApplicationParameters;
import appinstance.InstanceManager;
import audio.AudioEngineException;
import player.fx.playerwrapper.PlayerStatusWrapper;
import systemcontrol.LocalMachine;
import mediacommand.CombinationManager;
import mediacommand.MediaCommand;
import mediacommand.MediaCommandManager;
import player.model.MediaIndex;
import player.playback.PlaybackEngine;
import player.playback.PlayerStatus;
import vdp.VDP;

import javafx.application.Application;
import javafx.stage.Stage;

public class Launcher extends Application {
	private PlayerWindow window;

	@Override
	public void start(Stage primaryStage) throws Exception
	{
		ApplicationParameters appParams = new ApplicationParameters("Cyclops", getParameters());
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
		VDP vdp = new VDP();

		PlayerStatus status = new PlayerStatus(vdp);
		MediaIndex index = new MediaIndex(vdp, new File("index.jobjs"));

		PlaybackEngine engine = PlaybackEngine.initializeAudioEngine(status, null);

		window = new PlayerWindow(primaryStage, status, index, engine);
		window.show();

		addControl(window.getStatusWrapper());
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


	public static void addControl(PlayerStatusWrapper player) {
		if(MediaCommandManager.isSupported()) {
        	MediaCommandManager manager = MediaCommandManager.getInstance();
        	CombinationManager cm = new CombinationManager();
        	cm.register(manager);

        	cm.addCombination(new MediaCommand[]{ MediaCommand.PLAY_PAUSE }, c -> {
        		player.setPlaying(!player.isPlaying());
        	});
        	cm.addCombination(new MediaCommand[]{ MediaCommand.STOP }, c -> player.stop() );
        	cm.addCombination(new MediaCommand[]{ MediaCommand.NEXT }, c -> player.getStatus().next() );
        	cm.addCombination(new MediaCommand[]{ MediaCommand.PREVIOUS }, c -> player.getStatus().previous() );

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
        	cm.addCombination(nextCombination, c -> player.getStatus().next());
        	cm.addCombination(previousCombination, c -> player.getStatus().previous());
        	cm.addCombination(deleteCombination, c -> System.out.println("Delete not implemented yet"));
        }
	}

}
