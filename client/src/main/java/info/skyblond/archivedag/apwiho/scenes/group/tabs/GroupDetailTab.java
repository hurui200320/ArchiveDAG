package info.skyblond.archivedag.apwiho.scenes.group.tabs;

import info.skyblond.archivedag.apwiho.interfaces.BasicScene;
import info.skyblond.archivedag.apwiho.services.DialogService;
import info.skyblond.archivedag.apwiho.services.GrpcClientService;
import info.skyblond.archivedag.arudaz.protos.group.*;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.jetbrains.annotations.NotNull;

import java.sql.Timestamp;
import java.util.List;
import java.util.function.Consumer;

public class GroupDetailTab extends BasicScene {
    // TODO find a way to close this
    public static class MemberTableModel {
        public static List<TableColumn<MemberTableModel, ?>> getColumns() {
            TableColumn<MemberTableModel, String> username = new TableColumn<>("Member");
            username.setCellValueFactory(new PropertyValueFactory<>("username"));

            return List.of(username);
        }


        private final SimpleStringProperty username;

        public MemberTableModel(String username) {
            this.username = new SimpleStringProperty(username);
        }

        public String getUsername() {
            return this.username.get();
        }

        public SimpleStringProperty usernameProperty() {
            return this.username;
        }
    }

    public final String groupName;
    private Label groupInfoLabel;
    private TableView<MemberTableModel> tableView;
    private final Consumer<String> closeTab;

    public GroupDetailTab(String groupName, Consumer<String> closeTab) {
        this.groupName = groupName;
        this.closeTab = closeTab;
    }

    @Override
    protected @NotNull Parent generateLayout() {
        HBox root = new HBox(20);
        root.setAlignment(Pos.CENTER);

        this.groupInfoLabel = new Label();
        root.getChildren().add(this.groupInfoLabel);
        HBox.setMargin(this.groupInfoLabel, new Insets(0, 0, 0, 20));

        this.tableView = new TableView<>();
        root.getChildren().add(this.tableView);
        HBox.setHgrow(this.tableView, Priority.ALWAYS);
        this.tableView.setMaxHeight(Double.MAX_VALUE);
        this.tableView.getColumns().setAll(MemberTableModel.getColumns());

        VBox buttons = new VBox(10);
        root.getChildren().add(buttons);
        buttons.setAlignment(Pos.CENTER);
        HBox.setMargin(buttons, new Insets(0, 20, 0, 0));


        Button delete = new Button("Delete group");
        buttons.getChildren().add(delete);
        delete.setPrefWidth(125);
        delete.setOnAction(event -> this.deleteGroup());

        Button transfer = new Button("Transfer group");
        buttons.getChildren().add(transfer);
        transfer.setPrefWidth(125);
        transfer.setOnAction(event -> this.transferGroup());

        Button addMember = new Button("Join group");
        buttons.getChildren().add(addMember);
        addMember.setPrefWidth(125);
        addMember.setOnAction(event -> this.joinGroup());

        Button removeMember = new Button("Leave group");
        buttons.getChildren().add(removeMember);
        removeMember.setPrefWidth(125);
        removeMember.setOnAction(event -> this.leaveGroup());

        return root;
    }

    @Override
    protected void refreshLayout() {
        var alert = DialogService.getInstance().showWaitingDialog("Requesting info...");
        this.updateGroupInfoLabel();
        this.refreshMemberTableView();
        alert.close();
    }

    private void updateGroupInfoLabel() {
        try {
            var result = GrpcClientService.getInstance().getGroupServiceFutureStub()
                    .queryGroup(QueryGroupRequest.newBuilder()
                            .setGroupName(this.groupName)
                            .build()).get();
            var sb = new StringBuilder();
            sb.append("Group name: ").append(result.getGroupName()).append("\n\n");
            sb.append("Owner: ").append(result.getOwner()).append("\n\n");
            sb.append("Created at: ").append(new Timestamp(result.getCreatedTimestamp() * 1000));

            if (this.groupInfoLabel != null) {
                this.groupInfoLabel.setText(sb.toString());
            }
        } catch (Throwable t) {
            DialogService.getInstance().showExceptionDialog(t, "Failed to fetch group info.");
            if (this.groupInfoLabel != null) {
                this.groupInfoLabel.setText("Failed to fetch info");
            }
        }
    }

    private void refreshMemberTableView() {
        try {
            var result = GrpcClientService.getInstance().getGroupServiceFutureStub()
                    .listGroupMember(ListGroupMemberRequest.newBuilder()
                            .setGroupName(this.groupName)
                            .build()).get();
            var content = result.getUsernameList().stream().map(MemberTableModel::new).toList();
            if (this.tableView != null) {
                this.tableView.getItems().setAll(content);
                this.tableView.setVisible(true);
                this.tableView.setManaged(true);
            }
        } catch (Throwable t) {
            if (this.tableView != null) {
                this.tableView.setVisible(false);
                this.tableView.setManaged(false);
            }
        }
    }

    private void deleteGroup() {
        if (!DialogService.getInstance().showYesOrNoDialog("Delete group", "Delete " + this.groupName, "Really?")) {
            return;
        }
        try {
            GrpcClientService.getInstance().getGroupServiceFutureStub()
                    .deleteGroup(DeleteGroupRequest.newBuilder()
                            .setGroupName(this.groupName)
                            .build()).get();
            DialogService.getInstance().showInfoDialog("Group " + this.groupName + " has been deleted.", "Group deleted", "Success");
            this.closeTab.accept(this.groupName);
        } catch (Throwable t) {
            DialogService.getInstance().showExceptionDialog(t, "Failed to delete group");
        }
    }

    private void transferGroup() {
        var targetUsername = DialogService.getInstance().showTextInputDialog("Transfer group", "To which user?", "Username:", "");
        if (targetUsername == null) {
            return;
        }
        try {
            GrpcClientService.getInstance().getGroupServiceFutureStub()
                    .transferGroup(TransferGroupRequest.newBuilder()
                            .setGroupName(this.groupName)
                            .setNewOwner(targetUsername)
                            .build()).get();
            DialogService.getInstance().showInfoDialog("Transferred to " + targetUsername + " .", "Group transferred", "Success");
            this.refreshLayout();
        } catch (Throwable t) {
            DialogService.getInstance().showExceptionDialog(t, "Failed to transfer group");
        }
    }

    private void joinGroup() {
        var targetUsername = DialogService.getInstance().showTextInputDialog("Transfer group", "Add which user?", "Username:", "");
        if (targetUsername == null) {
            return;
        }
        try {
            GrpcClientService.getInstance().getGroupServiceFutureStub()
                    .joinGroup(JoinGroupRequest.newBuilder()
                            .setGroupName(this.groupName)
                            .setNewMember(targetUsername)
                            .build()).get();
            DialogService.getInstance().showInfoDialog("Added user " + targetUsername + ".", "Member added", "Success");
            this.refreshLayout();
        } catch (Throwable t) {
            DialogService.getInstance().showExceptionDialog(t, "Failed to add user");
        }
    }

    private void leaveGroup() {
        var targetMember = this.tableView.getSelectionModel().getSelectedItem();
        if (targetMember == null) {
            return;
        }
        if (!DialogService.getInstance().showYesOrNoDialog("Remove member",
                "Remove " + targetMember.getUsername(), "Really?")) {
            return;
        }
        try {
            GrpcClientService.getInstance().getGroupServiceFutureStub()
                    .leaveGroup(LeaveGroupRequest.newBuilder()
                            .setGroupName(this.groupName)
                            .setLeavingMember(targetMember.getUsername())
                            .build()).get();
            DialogService.getInstance().showInfoDialog("Removed user " + targetMember.getUsername() + ".", "Member removed", "Success");
            this.refreshLayout();
        } catch (Throwable t) {
            DialogService.getInstance().showExceptionDialog(t, "Failed to remove user");
        }
    }
}
