package info.skyblond.archivedag.apwiho;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class Main extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws IOException {
        Parent parent = new FXMLLoader(this.getClass().getResource("/view/Login.fxml")).load();
        primaryStage.setTitle("ArchiveDAG Client");
        primaryStage.setScene(new Scene(parent));
        primaryStage.show();
    }
}
