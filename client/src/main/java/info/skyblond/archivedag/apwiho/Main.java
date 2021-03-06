package info.skyblond.archivedag.apwiho;

import info.skyblond.archivedag.apwiho.scenes.LoginScene;
import info.skyblond.archivedag.apwiho.services.SlicerService;
import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class Main extends Application {
    public static final String DEFAULT_TITLE = "ArchiveDAG Client";
    private static Stage stage = null;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        Scene scene = new Scene(new Label("???"), 800, 600);
        LoginScene loginScene = new LoginScene(scene);
        Parent parent = loginScene.renderRoot();
        scene.setRoot(parent);
        primaryStage.setScene(scene);
        primaryStage.setTitle(DEFAULT_TITLE);
        // save the stage
        stage = primaryStage;

        primaryStage.setOnCloseRequest(event -> {
            event.consume(); // TODO
            try {
                SlicerService.getInstance().close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            primaryStage.close();
        });

        primaryStage.show();
    }

    public static void changeTitle(String newTitle) {
        if (stage != null) {
            stage.setTitle(newTitle);
        }
    }
}
