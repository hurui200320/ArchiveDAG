package info.skyblond.archivedag.apwiho.scenes.group.tabs;

import info.skyblond.archivedag.apwiho.interfaces.BasicScene;
import info.skyblond.archivedag.apwiho.scenes.group.GroupTableModel;
import info.skyblond.archivedag.apwiho.scenes.templates.FiniteTableView;
import info.skyblond.archivedag.apwiho.services.DialogService;
import info.skyblond.archivedag.apwiho.services.GrpcClientService;
import info.skyblond.archivedag.apwiho.services.JavaFXUtils;
import info.skyblond.archivedag.arudaz.protos.group.ListGroupNameRequest;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;

public class SearchGroupTab extends BasicScene {

    private Label warningLabel;
    private FiniteTableView<GroupTableModel> tableView;
    private TextField keywordTextField;
    private final int maxSize = 1000;
    private final BiConsumer<String, GroupDetailTab> addNewTab;

    public SearchGroupTab(BiConsumer<String, GroupDetailTab> addNewTab) {
        this.addNewTab = addNewTab;
    }

    @Override
    protected @NotNull Parent generateLayout() {
        VBox root = new VBox();

        HBox keywordContainer = new HBox(5);
        keywordContainer.setMaxWidth(Double.MAX_VALUE);
        root.getChildren().add(keywordContainer);
        this.keywordTextField = new TextField();
        this.keywordTextField.setPromptText("Keyword filter");
        HBox.setHgrow(this.keywordTextField, Priority.ALWAYS);
        this.keywordTextField.textProperty().addListener((ob, ov, nv) -> this.refreshLayout());
        keywordContainer.getChildren().add(this.keywordTextField);

        this.warningLabel = new Label("Too many records! \nNot all records are displayed here.\nYou need to adjust the filter to reduce the result.");
        this.warningLabel.setVisible(false);
        this.warningLabel.setManaged(false);
        root.getChildren().add(this.warningLabel);

        this.tableView = new FiniteTableView<>(this.maxSize,
                tableView -> {
                    tableView.getColumns().setAll(GroupTableModel.getColumns());
                    VBox.setVgrow(tableView, Priority.ALWAYS);
                    JavaFXUtils.setTableViewDoubleAction(tableView, r -> this.addNewTab.accept(r.getGroupName(), new GroupDetailTab(r.getGroupName())));
                }
        );
        this.tableView.isTooManyRecords.addListener((observable, oldValue, newValue) -> {
            this.warningLabel.setVisible(newValue);
            this.warningLabel.setManaged(newValue);
        });
        root.getChildren().add(this.tableView.tableView);

        return root;
    }

    @Override
    protected void refreshLayout() {
        this.tableView.setContent(this.refreshResult());
    }

    private List<GroupTableModel> refreshResult() {
        try {
            return GroupTableModel.resolveFromGroupNameList(GrpcClientService.getInstance().getGroupServiceFutureStub()
                    .listGroupName(ListGroupNameRequest.newBuilder()
                            .setKeyword(this.keywordTextField.getText())
                            .setLimit(this.maxSize)
                            .build()).get().getGroupNameList());
        } catch (Throwable t) {
            DialogService.getInstance().showExceptionDialog(t, "Failed to search groups");
            return Collections.emptyList();
        }
    }
}
