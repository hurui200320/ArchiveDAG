package info.skyblond.archivedag.apwiho.scenes.file.tabs;

import info.skyblond.archivedag.apwiho.interfaces.SwappableScene;
import info.skyblond.archivedag.apwiho.scenes.file.FileRecordDetailScene;
import info.skyblond.archivedag.apwiho.scenes.file.FileRecordManagementScene;
import info.skyblond.archivedag.apwiho.scenes.file.FileRecordTableModel;
import info.skyblond.archivedag.apwiho.scenes.templates.PageableTableView;
import info.skyblond.archivedag.apwiho.services.DialogService;
import info.skyblond.archivedag.apwiho.services.GrpcClientService;
import info.skyblond.archivedag.apwiho.services.JavaFXUtils;
import javafx.scene.Parent;
import javafx.scene.Scene;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;

public class UserSharedRecordTab extends SwappableScene {

    private PageableTableView<FileRecordTableModel> pageableTableView;
    private final FileRecordManagementScene rootScene;

    public UserSharedRecordTab(Scene currentScene, FileRecordManagementScene rootScene) {
        super(currentScene);
        this.rootScene = rootScene;
    }

    @Override
    protected @NotNull Parent generateLayout() {
        this.pageableTableView = new PageableTableView<>(
                20, -1,
                page -> {
                    var alert = DialogService.getInstance().showWaitingDialog("Requesting info...");
                    try {
                        return FileRecordTableModel.resolveFromRecordUUIDList(
                                GrpcClientService.getInstance().getFileRecordServiceFutureStub()
                                        .listUserSharedFileRecords(page).get().getRecordUuidList());
                    } catch (Throwable t) {
                        DialogService.getInstance().showExceptionDialog(t, "Failed to list user shared file records");
                        return Collections.emptyList();
                    } finally {
                        alert.close();
                    }
                },
                tableView -> {
                    tableView.getColumns().setAll(FileRecordTableModel.getColumns());
                    JavaFXUtils.setTableViewDoubleAction(tableView, rowData -> this.swapTo(new FileRecordDetailScene(
                            rowData.getRecordUUID(),
                            this.getCurrentScene(),
                            this.rootScene
                    )));
                }
        );

        return this.pageableTableView.renderRoot();
    }

    @Override
    protected void refreshLayout() {
        this.pageableTableView.renderRoot();
    }

}
