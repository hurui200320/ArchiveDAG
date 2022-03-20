package info.skyblond.archivedag.apwiho.scenes.file.tabs;

import info.skyblond.archivedag.apwiho.interfaces.SwappableScene;
import info.skyblond.archivedag.apwiho.scenes.file.FileRecordDetailScene;
import info.skyblond.archivedag.apwiho.scenes.file.FileRecordManagementScene;
import info.skyblond.archivedag.apwiho.services.DialogService;
import info.skyblond.archivedag.apwiho.services.GrpcClientService;
import info.skyblond.archivedag.arudaz.protos.record.CreateFileRecordRequest;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.jetbrains.annotations.NotNull;

public class CreateFileRecordTab extends SwappableScene {

    private TextField recordNameTextField;
    private final FileRecordManagementScene rootScene;

    public CreateFileRecordTab(Scene currentScene, FileRecordManagementScene rootScene) {
        super(currentScene);
        this.rootScene = rootScene;
    }

    @Override
    protected @NotNull Parent generateLayout() {
        VBox root = new VBox(10);
        root.setAlignment(Pos.CENTER);
        root.setMaxWidth(350);

        Label recordNameLabel = new Label("Record name:");
        root.getChildren().add(recordNameLabel);
        this.recordNameTextField = new TextField();
        root.getChildren().add(this.recordNameTextField);

        Button button = new Button("Create");
        root.getChildren().add(button);
        button.setOnAction(e -> this.createNewRecord());

        HBox center = new HBox();
        center.setAlignment(Pos.CENTER);
        center.getChildren().add(root);
        return center;
    }

    @Override
    protected void refreshLayout() {
    }

    private void createNewRecord() {
        var alert = DialogService.getInstance().showWaitingDialog("Processing...");
        try {
            var result = GrpcClientService.getInstance().getFileRecordServiceFutureStub()
                    .createFileRecord(CreateFileRecordRequest.newBuilder()
                            .setRecordName(this.recordNameTextField.getText())
                            .build())
                    .get();
            this.recordNameTextField.clear();
            this.swapTo(new FileRecordDetailScene(result.getRecordUuid(0), this.getCurrentScene(), this.rootScene));
        } catch (Throwable t) {
            DialogService.getInstance().showExceptionDialog(t, "Failed to create file record.");
        } finally {
            alert.close();
        }
    }
}
