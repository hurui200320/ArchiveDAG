package info.skyblond.archivedag.apwiho.scenes.file.tabs;

import info.skyblond.archivedag.apwiho.interfaces.SwappableScene;
import info.skyblond.archivedag.apwiho.scenes.file.FileRecordManagementScene;
import info.skyblond.archivedag.apwiho.scenes.file.FileRecordTableModel;
import info.skyblond.archivedag.apwiho.scenes.file.details.FileRecordDetailScene;
import info.skyblond.archivedag.apwiho.scenes.templates.PageableTableView;
import info.skyblond.archivedag.apwiho.services.DialogService;
import info.skyblond.archivedag.apwiho.services.GrpcClientService;
import info.skyblond.archivedag.apwiho.services.JavaFXUtils;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;

public class PublicSharedRecordTab extends SwappableScene {

    private PageableTableView<FileRecordTableModel> pageableTableView;
    private TextField nameSearch;
    private final FileRecordManagementScene rootScene;

    public PublicSharedRecordTab(Scene currentScene, FileRecordManagementScene rootScene) {
        super(currentScene);
        this.rootScene = rootScene;
    }

    @Override
    protected @NotNull Parent generateLayout() {
        VBox root = new VBox();

        this.nameSearch = new TextField();
        root.getChildren().add(this.nameSearch);
        this.nameSearch.setPromptText("Name highlight search");

        this.pageableTableView = new PageableTableView<>(
                20, -1,
                page -> {
                    var alert = DialogService.getInstance().showWaitingDialog("Requesting info...");
                    try {
                        return FileRecordTableModel.resolveFromRecordUUIDList(
                                GrpcClientService.getInstance().getFileRecordServiceFutureStub()
                                        .listPublicSharedFileRecords(page).get().getRecordUuidList());
                    } catch (Throwable t) {
                        DialogService.getInstance().showExceptionDialog(t, "Failed to list public shared file records");
                        return Collections.emptyList();
                    } finally {
                        alert.close();
                    }
                },
                tableView -> {
                    tableView.getColumns().setAll(FileRecordTableModel.getColumns());
                    JavaFXUtils.setTableViewWithFactory(tableView,
                            JavaFXUtils.setRowOnDoubleClick(r -> this.swapTo(new FileRecordDetailScene(
                                    r.getRecordUUID(),
                                    this.getCurrentScene(),
                                    this.rootScene
                            ))),
                            JavaFXUtils.setRowOnHighlightChange(this.nameSearch.textProperty(),
                                    FileRecordTableModel::getRecordName));
                }
        );
        var tableView = this.pageableTableView.renderRoot();
        root.getChildren().add(tableView);
        VBox.setVgrow(tableView, Priority.ALWAYS);

        return root;
    }

    @Override
    protected void refreshLayout() {
        this.pageableTableView.renderRoot();
    }

}
