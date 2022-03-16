package info.skyblond.archivedag.apwiho.scenes.group.tabs;

import info.skyblond.archivedag.apwiho.interfaces.BasicScene;
import info.skyblond.archivedag.apwiho.services.DialogService;
import info.skyblond.archivedag.apwiho.services.GrpcClientService;
import info.skyblond.archivedag.arudaz.protos.group.CreateGroupRequest;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiConsumer;

public class CreateGroupTab extends BasicScene {

    private TextField groupNameTextField;
    private TextField ownerTextField;
    private final BiConsumer<String, GroupDetailTab> addNewTab;

    public CreateGroupTab(BiConsumer<String, GroupDetailTab> addNewTab) {
        this.addNewTab = addNewTab;
    }

    @Override
    protected @NotNull Parent generateLayout() {
        VBox root = new VBox(10);
        root.setAlignment(Pos.CENTER);
        root.setMaxWidth(350);

        Label groupNameLabel = new Label("Group name:");
        root.getChildren().add(groupNameLabel);
        this.groupNameTextField = new TextField();
        root.getChildren().add(this.groupNameTextField);


        Label ownerLabel = new Label("Owner: (Empty means current user)");
        root.getChildren().add(ownerLabel);
        this.ownerTextField = new TextField();
        root.getChildren().add(this.ownerTextField);

        Button button = new Button("Create");
        root.getChildren().add(button);
        button.setOnAction(e -> this.createNewGroup());

        HBox center = new HBox();
        center.setAlignment(Pos.CENTER);
        center.getChildren().add(root);
        return center;
    }

    @Override
    protected void refreshLayout() {
    }

    private void createNewGroup() {
        var alert = DialogService.getInstance().showWaitingDialog("Processing...");
        try {
            GrpcClientService.getInstance().getGroupServiceFutureStub()
                    .createGroup(CreateGroupRequest.newBuilder()
                            .setGroupName(this.groupNameTextField.getText())
                            .setOwner(this.ownerTextField.getText())
                            .build())
                    .get();
            this.addNewTab.accept(this.groupNameTextField.getText(), new GroupDetailTab(this.groupNameTextField.getText()));
            this.groupNameTextField.clear();
            this.ownerTextField.clear();
        } catch (Throwable t) {
            DialogService.getInstance().showExceptionDialog(t, "Failed to create group.");
        } finally {
            alert.close();
        }
    }
}
