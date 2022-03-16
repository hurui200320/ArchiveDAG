package info.skyblond.archivedag.apwiho.scenes.group;

import info.skyblond.archivedag.apwiho.interfaces.BasicScene;
import info.skyblond.archivedag.apwiho.interfaces.Renderable;
import info.skyblond.archivedag.apwiho.services.GrpcClientService;
import info.skyblond.archivedag.arudaz.protos.group.ListJoinedGroupRequest;
import javafx.scene.Parent;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;

@SuppressWarnings("DuplicatedCode")
public class JoinedGroupTab extends BasicScene {

    private TableView<GroupTableModel> tableView;
    private TextField usernameTextField;
    private final BiConsumer<String, Renderable> addNewTab;

    public JoinedGroupTab(BiConsumer<String, Renderable> addNewTab) {
        this.addNewTab = addNewTab;
    }

    @Override
    protected @NotNull Parent generateLayout() {
        VBox root = new VBox();

        HBox keywordContainer = new HBox(5);
        keywordContainer.setMaxWidth(Double.MAX_VALUE);
        root.getChildren().add(keywordContainer);
        this.usernameTextField = new TextField();
        this.usernameTextField.setPromptText("Username filter, leave empty to refer current user");
        HBox.setHgrow(this.usernameTextField, Priority.ALWAYS);
        this.usernameTextField.textProperty().addListener((ob, ov, nv) -> this.refreshLayout());
        keywordContainer.getChildren().add(this.usernameTextField);

        this.tableView = new TableView<>();

        this.tableView.getColumns().setAll(GroupTableModel.getColumns());
        VBox.setVgrow(this.tableView, Priority.ALWAYS);
        this.tableView.setRowFactory(t -> {
            TableRow<GroupTableModel> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    GroupTableModel rowData = row.getItem();
                    this.addNewTab.accept(rowData.getGroupName(), new GroupDetailTab(rowData.getGroupName()));
                }
            });
            return row;
        });
        root.getChildren().add(this.tableView);

        return root;
    }

    @Override
    protected void refreshLayout() {
        this.tableView.getItems().setAll(this.refreshResult());
    }

    private List<GroupTableModel> refreshResult() {
        try {
            return GroupTableModel.resolveFromGroupNameList(GrpcClientService.getInstance().getGroupServiceFutureStub()
                    .listJoinedGroup(ListJoinedGroupRequest.newBuilder()
                            .setUsername(this.usernameTextField.getText())
                            .build()).get().getGroupNameList());
        } catch (Throwable t) {
            return Collections.emptyList();
        }
    }
}
