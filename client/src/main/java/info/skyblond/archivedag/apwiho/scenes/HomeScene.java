package info.skyblond.archivedag.apwiho.scenes;

import info.skyblond.archivedag.apwiho.interfaces.SwappableScene;
import info.skyblond.archivedag.apwiho.scenes.file.FileRecordManagementScene;
import info.skyblond.archivedag.apwiho.scenes.group.GroupManagementScene;
import info.skyblond.archivedag.apwiho.scenes.user.UserInfoScene;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import org.jetbrains.annotations.NotNull;

public class HomeScene extends SwappableScene {

    public HomeScene(Scene currentScene) {
        super(currentScene);
    }

    private Button generateButton(String buttonTitle, EventHandler<ActionEvent> buttonCallback) {
        Button result = new Button(buttonTitle);
        result.setPrefWidth(275);
        result.setFont(Font.font(16));
        result.setOnAction(buttonCallback);
        return result;
    }

    private UserInfoScene userInfoScene;
    private GroupManagementScene groupManagementScene;
    private FileRecordManagementScene fileRecordManagementScene;

    @Override
    protected @NotNull Parent generateLayout() {
        BorderPane root = new BorderPane();
        VBox top = new VBox();
        top.setAlignment(Pos.CENTER_LEFT);
        root.setTop(top);
        Label topPath = new Label("Home");
        VBox.setMargin(topPath, new Insets(10, 10, 10, 10));
        topPath.setFont(Font.font(18));
        top.getChildren().add(topPath);

        VBox center = new VBox(30);
        center.setAlignment(Pos.CENTER);
        root.setCenter(center);
        center.getChildren().add(this.generateButton("User info", e -> {
            Button b = (Button) e.getSource();
            b.setDisable(true);
            if (this.userInfoScene == null) {
                this.userInfoScene = new UserInfoScene(this.getCurrentScene(), this);
            }
            this.swapTo(this.userInfoScene);
            b.setDisable(false);
        }));
        center.getChildren().add(this.generateButton("Group management", e -> {
            Button b = (Button) e.getSource();
            b.setDisable(true);
            if (this.groupManagementScene == null) {
                this.groupManagementScene = new GroupManagementScene(this.getCurrentScene(), this);
            }
            this.swapTo(this.groupManagementScene);
            b.setDisable(false);
        }));
        center.getChildren().add(this.generateButton("File record management", e -> {
            Button b = (Button) e.getSource();
            b.setDisable(true);
            if (this.fileRecordManagementScene == null) {
                this.fileRecordManagementScene = new FileRecordManagementScene(this.getCurrentScene(), this);
            }
            this.swapTo(this.fileRecordManagementScene);
            b.setDisable(false);
        }));
        return root;
    }

    @Override
    protected void refreshLayout() {
        // nothing to refresh
    }
}
