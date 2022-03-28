package info.skyblond.archivedag.apwiho.scenes.group.tabs;

import info.skyblond.archivedag.apwiho.interfaces.BasicScene;
import info.skyblond.archivedag.apwiho.scenes.group.GroupTableModel;
import info.skyblond.archivedag.apwiho.services.GrpcClientService;
import info.skyblond.archivedag.apwiho.services.JavaFXUtils;
import info.skyblond.archivedag.arudaz.protos.group.ListOwnedGroupRequest;
import javafx.scene.Parent;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class OwnedGroupTab extends BasicScene {

    private TableView<GroupTableModel> tableView;
    private TextField usernameTextField;
    private final BiConsumer<String, GroupDetailTab> addNewTab;
    private final Consumer<String> closeTab;

    public OwnedGroupTab(BiConsumer<String, GroupDetailTab> addNewTab, Consumer<String> closeTab) {
        this.addNewTab = addNewTab;
        this.closeTab = closeTab;
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
        JavaFXUtils.setTableViewDoubleAction(this.tableView, r -> this.addNewTab.accept(r.getGroupName(),
                new GroupDetailTab(r.getGroupName(), this.closeTab)));
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
                    .listOwnedGroup(ListOwnedGroupRequest.newBuilder()
                            .setUsername(this.usernameTextField.getText())
                            .build()).get().getGroupNameList());
        } catch (Throwable t) {
            return Collections.emptyList();
        }
    }
}
