package info.skyblond.archivedag.apwiho;

import info.skyblond.archivedag.apwiho.scenes.LoginScene;
import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    public static final boolean DEBUG = true;

    public static void main(String[] args) {
        if (DEBUG) {
            System.err.println("DEBUG mode enabled!");
        }
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        Scene scene = new Scene(new Group(), 800, 600);
        LoginScene loginScene = new LoginScene(scene);
        Parent parent = loginScene.renderRoot();
        scene.setRoot(parent);
        primaryStage.setScene(scene);
        primaryStage.setTitle("ArchiveDAG Client");

        primaryStage.setOnCloseRequest(event -> {
            event.consume(); // TODO
            System.out.println("TODO: Implementing the close check");
            System.out.println("Closing windows...");
            primaryStage.close();
        });

        primaryStage.show();
    }
}
