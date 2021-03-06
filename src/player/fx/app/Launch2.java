package player.fx.app;

import appinstance.ApplicationParameters;
import appinstance.InstanceManager;
import audio.AudioEngineException;
import cloud.Cloud;
import cloud.Peer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import player.extensions.debug.CloudViewer;
import player.model.CycloneConfig;
import player.model.PlaylistPlayer;
import player.model.playback.PlaybackEngine;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static player.model.ConfigKt.getConfigFile;

public class Launch2 extends Application {
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

		Peer.getLocal().setId("1");
		Peer.getLocal().setName("peer1");
		Cloud cloud1 = new Cloud();
		cloud1.read(getConfigFile("status.cld"), true);

		Cloud cloud2 = new Cloud();
		cloud2.initLocalPeer$Cyclone(new Peer(true, "peer2", "localhost", "2"));

		Stage v1Stage = new Stage();
		v1Stage.setTitle("1");
		CloudViewer viewer1 = new CloudViewer(cloud1);
		viewer1.show(v1Stage);
		Stage v2Stage = new Stage();
		v2Stage.setTitle("2");
		CloudViewer viewer2 = new CloudViewer(cloud2);
		v2Stage.setX(800);
		viewer2.show(v2Stage);



		PlaybackEngine engine = new PlaybackEngine(cloud1, config);

		PlaylistPlayer player = new PlaylistPlayer(cloud1, config);
		window = new PlayerWindow(primaryStage, player, engine, config);
		window.show();


		if (config.getConnectOnStartup().get()) {
			cloud1.connect(config.getMulticastAddress().get(), config.getMulticastPort().get(), true, 1000);
		}

//			TaskViewer viewer = new TaskViewer(cloud, new Stage());
//			viewer.getStage().show();
//			PlaybackViewer pbv = new PlaybackViewer(engine);
//			pbv.getStage().show();
//			CloudViewer cv = new CloudViewer(cloud);
//			cv.getStage().show();



			PlaylistPlayer player2 = new PlaylistPlayer(cloud2, config);
			PlayerWindow window2 = new PlayerWindow(new Stage(), player2, engine, config);
			window2.getStage().setTitle("2");
			window2.getStage().setX(window2.getStage().getX() + 300);
			window2.show();

			cloud1.connect(config.getMulticastAddress().get(), config.getMulticastPort().get(), true, 1000);
			new Thread(() -> {
				try {
					Thread.sleep(1000);
					cloud2.connect(config.getMulticastAddress().get(), config.getMulticastPort().get(), true, 1000);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}).start();

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
}
