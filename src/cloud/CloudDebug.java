package cloud;

import javafx.application.Application;
import javafx.stage.Stage;
import player.fx.debug.CloudViewer;

public class CloudDebug extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        Peer.getLocal().setId("1");
        Peer.getLocal().setName("peer1");
        Cloud cloud1 = new Cloud();

        Cloud cloud2 = new Cloud();
        cloud2.setPeer$Cyclone(new Peer(true, "peer2", "localhost", "2"));

        CloudViewer viewer1 = new CloudViewer(cloud1, "1");
        viewer1.getStage().show();

        CloudViewer viewer2 = new CloudViewer(cloud2, "2");
        viewer2.getStage().setX(800);
        viewer2.getStage().show();

        cloud1.connect("225.4.5.6", 5324, 5325);
        new Thread(() -> {
            try {
                Thread.sleep(1000);
                cloud2.connect("225.4.5.6", 5324, 5335);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();


    }

    public static void main(String[] args) {
        launch(args);
    }
}
