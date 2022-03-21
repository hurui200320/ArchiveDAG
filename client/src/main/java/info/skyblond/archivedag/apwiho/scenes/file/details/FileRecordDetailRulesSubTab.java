package info.skyblond.archivedag.apwiho.scenes.file.details;

import info.skyblond.archivedag.apwiho.interfaces.BasicScene;
import info.skyblond.archivedag.apwiho.services.DialogService;
import info.skyblond.archivedag.apwiho.services.GrpcClientService;
import info.skyblond.archivedag.arudaz.protos.record.DeleteSharedRuleForRecordRequest;
import info.skyblond.archivedag.arudaz.protos.record.ListSharedRulesForRecordRequest;
import info.skyblond.archivedag.arudaz.protos.record.SharedRule;
import info.skyblond.archivedag.arudaz.protos.record.SharedRuleType;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class FileRecordDetailRulesSubTab extends BasicScene {

    private final String recordUUID;

    public FileRecordDetailRulesSubTab(String recordUUID) {
        this.recordUUID = recordUUID;
    }

    public static class RuleTableModel {
        public static List<TableColumn<RuleTableModel, ?>> getColumns() {
            TableColumn<RuleTableModel, String> type = new TableColumn<>("Type");
            type.setCellValueFactory(new PropertyValueFactory<>("type"));

            TableColumn<RuleTableModel, String> target = new TableColumn<>("Target");
            target.setCellValueFactory(new PropertyValueFactory<>("target"));

            TableColumn<RuleTableModel, String> permission = new TableColumn<>("Permission");
            permission.setCellValueFactory(new PropertyValueFactory<>("permission"));

            return List.of(type, target, permission);
        }

        private final SimpleStringProperty type;
        private final SimpleStringProperty target;
        private final SimpleStringProperty permission;

        public RuleTableModel(SharedRule proto) {
            this.type = new SimpleStringProperty(proto.getRuleType().toString());
            this.target = new SimpleStringProperty(proto.getRuleTarget());
            this.permission = new SimpleStringProperty(proto.getPermission());
        }

        public String getType() {
            return this.type.get();
        }

        public SimpleStringProperty typeProperty() {
            return this.type;
        }

        public String getTarget() {
            return this.target.get();
        }

        public SimpleStringProperty targetProperty() {
            return this.target;
        }

        public String getPermission() {
            return this.permission.get();
        }

        public SimpleStringProperty permissionProperty() {
            return this.permission;
        }
    }

    private TableView<RuleTableModel> tableView;

    @Override
    protected @NotNull Parent generateLayout() {
        HBox root = new HBox(20);
        root.setAlignment(Pos.CENTER);

        this.tableView = new TableView<>();
        root.getChildren().add(this.tableView);
        HBox.setHgrow(this.tableView, Priority.ALWAYS);
        this.tableView.setMaxHeight(Double.MAX_VALUE);
        this.tableView.getColumns().setAll(RuleTableModel.getColumns());

        VBox buttons = new VBox(10);
        root.getChildren().add(buttons);
        buttons.setAlignment(Pos.CENTER);
        HBox.setMargin(buttons, new Insets(0, 20, 0, 0));

        Button set = new Button("Set");
        buttons.getChildren().add(set);
        set.setPrefWidth(125);
        set.setOnAction(event -> this.setRule());

        Button remove = new Button("Remove");
        buttons.getChildren().add(remove);
        remove.setPrefWidth(125);
        remove.setOnAction(event -> this.removeRule());

        return root;
    }

    private void setRule() {
        try {
            var typeString = DialogService.getInstance().showTextInputDialog("Set rule", "Which type? (USER, GROUP, OTHER)", "Type:", "");
            if (typeString == null) {
                return;
            }
            var type = SharedRuleType.valueOf(typeString.toUpperCase());
            var target = "";
            if (type != SharedRuleType.OTHER) {
                target = DialogService.getInstance().showTextInputDialog("Set rule", "For who?", "Target:", "");
                if (target == null) {
                    return;
                }
            }
            var permission = DialogService.getInstance().showTextInputDialog("Set rule", "What permission? (r, u, n)", "Permission:", "");
            if (permission == null) {
                return;
            }

            GrpcClientService.getInstance().getFileRecordServiceFutureStub()
                    .setSharedRuleForRecord(SharedRule.newBuilder()
                            .setRecordUuid(this.recordUUID)
                            .setRuleType(type)
                            .setRuleTarget(target)
                            .setPermission(permission)
                            .build()).get();
            DialogService.getInstance().showInfoDialog("Set rule.", "Rule set", "Success");
            this.refreshLayout();
        } catch (Throwable t) {
            DialogService.getInstance().showExceptionDialog(t, "Failed to set rule");
        }
    }

    private void removeRule() {
        var targetRule = this.tableView.getSelectionModel().getSelectedItem();
        if (targetRule == null) {
            return;
        }
        if (!DialogService.getInstance().showYesOrNoDialog("Remove rule",
                "Remove " + targetRule.getPermission() + "for " + targetRule.getType() + " " + targetRule.getTarget(), "Really?")) {
            return;
        }
        try {
            GrpcClientService.getInstance().getFileRecordServiceFutureStub()
                    .deleteSharedRuleForRecord(DeleteSharedRuleForRecordRequest.newBuilder()
                            .setRecordUuid(this.recordUUID)
                            .setRuleType(SharedRuleType.valueOf(targetRule.getType()))
                            .setRuleTarget(targetRule.getTarget())
                            .build()).get();
            DialogService.getInstance().showInfoDialog("Removed rule for " + targetRule.getType() + " " + targetRule.getTarget() + ".", "Rule removed", "Success");
            this.refreshLayout();
        } catch (Throwable t) {
            DialogService.getInstance().showExceptionDialog(t, "Failed to remove rule");
        }
    }

    @Override
    protected void refreshLayout() {
        var alert = DialogService.getInstance().showWaitingDialog("Requesting info...");
        this.refreshTableView();
        alert.close();
    }

    private void refreshTableView() {
        try {
            var result = GrpcClientService.getInstance().getFileRecordServiceFutureStub()
                    .listSharedRulesForRecord(ListSharedRulesForRecordRequest.newBuilder()
                            .setRecordUuid(this.recordUUID)
                            .build()).get();
            var content = result.getSharedRuleList().stream().map(RuleTableModel::new).toList();
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
}
