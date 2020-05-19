package cloud;

import javafx.application.Application;
import javafx.stage.Stage;
import player.extensions.debug.CloudViewer;

public class CloudDebug extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        Peer.getLocal().setId("1");
        Peer.getLocal().setName("peer1");
        Cloud cloud1 = new Cloud();

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

        cloud1.connect("225.139.25.1", 5324, true, 1000);
//        new Thread(() -> {
//            try {
//                Thread.sleep(1000);
//                cloud2.connect("225.4.5.6", 5324, 5335, true);
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }).start();


    }

    public static void main(String[] args) {
        launch(args);
    }
}
