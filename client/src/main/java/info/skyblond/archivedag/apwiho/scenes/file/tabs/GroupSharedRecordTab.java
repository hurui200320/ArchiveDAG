package info.skyblond.archivedag.apwiho.scenes.file.tabs;

import info.skyblond.archivedag.apwiho.interfaces.SwappableScene;
import info.skyblond.archivedag.apwiho.scenes.file.FileRecordDetailScene;
import info.skyblond.archivedag.apwiho.scenes.file.FileRecordManagementScene;
import info.skyblond.archivedag.apwiho.scenes.file.FileRecordTableModel;
import info.skyblond.archivedag.apwiho.scenes.templates.PageableTableView;
import info.skyblond.archivedag.apwiho.services.DialogService;
import info.skyblond.archivedag.apwiho.services.GrpcClientService;
import info.skyblond.archivedag.apwiho.services.JavaFXUtils;
import info.skyblond.archivedag.arudaz.protos.common.Empty;
import info.skyblond.archivedag.arudaz.protos.record.ListGroupSharedFileRecordsRequest;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

// Current scene won't swap to others, but the sub tabs will.
public class GroupSharedRecordTab extends SwappableScene {

    private TabPane tabPane;
    private final FileRecordManagementScene rootScene;
    private String lastSelectedGroupName;

    public GroupSharedRecordTab(Scene currentScene, FileRecordManagementScene rootScene) {
        super(currentScene);
        this.rootScene = rootScene;
    }

    @Override
    protected @NotNull Parent generateLayout() {
        this.tabPane = new TabPane();
        // allow use all spaces
        this.tabPane.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        // cannot reorder tabs
        this.tabPane.setTabDragPolicy(TabPane.TabDragPolicy.FIXED);
        this.lastSelectedGroupName = null;
        // refresh tab when selected
        this.tabPane.getSelectionModel().selectedItemProperty()
                .addListener((observable, oldValue, newTab) -> {
                    if (newTab != null) {
                        var renderable = (GroupSharedSubTab) newTab.getUserData();
                        // save the last selected group
                        this.lastSelectedGroupName = renderable.groupName;
                        // refresh the new tab by render it again
                        newTab.setContent(renderable.renderRoot());
                    }
                });

        return this.tabPane;
    }

    private static class GroupSharedSubTab extends SwappableScene {
        private final FileRecordManagementScene rootScene;
        public final String groupName;

        public GroupSharedSubTab(String groupName, Scene currentScene, FileRecordManagementScene rootScene) {
            super(currentScene);
            this.rootScene = rootScene;
            this.groupName = groupName;
        }

        private PageableTableView<FileRecordTableModel> pageableTableView;

        @Override
        protected @NotNull Parent generateLayout() {
            this.pageableTableView = new PageableTableView<>(
                    20, -1,
                    page -> {
                        var alert = DialogService.getInstance().showWaitingDialog("Requesting info...");
                        try {
                            return FileRecordTableModel.resolveFromRecordUUIDList(
                                    GrpcClientService.getInstance().getFileRecordServiceFutureStub()
                                            .listGroupSharedFileRecords(ListGroupSharedFileRecordsRequest.newBuilder()
                                                    .setGroupName(this.groupName)
                                                    .setPagination(page)
                                                    .build()).get().getRecordUuidList());
                        } catch (Throwable t) {
                            DialogService.getInstance().showExceptionDialog(t, "Failed to list group shared file records");
                            return Collections.emptyList();
                        } finally {
                            alert.close();
                        }
                    },
                    tableView -> {
                        tableView.getColumns().setAll(FileRecordTableModel.getColumns());
                        JavaFXUtils.setTableViewDoubleAction(tableView, r -> this.swapTo(new FileRecordDetailScene(
                                r.getRecordUUID(),
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

    @Override
    protected void refreshLayout() {
        var alert = DialogService.getInstance().showWaitingDialog("Requesting info...");
        List<String> joinedGroups;
        try {
            // get all groups
            joinedGroups = GrpcClientService.getInstance().getUserInfoServiceFutureStub().whoAmI(Empty.getDefaultInstance()).get().getJoinedGroupList();
        } catch (Throwable t) {
            DialogService.getInstance().showExceptionDialog(t, "Failed to list joined group");
            joinedGroups = List.of();
        } finally {
            alert.close();
        }
        // for each group, generate sub tab
        var tabList = joinedGroups.stream().map(group -> {
            Tab tab = new Tab(group);
            tab.setClosable(false);

            GroupSharedSubTab tabContent = new GroupSharedSubTab(group, this.getCurrentScene(), this.rootScene);

            tab.setUserData(tabContent);
            tab.setContent(tabContent.renderRoot());

            return tab;
        }).toList();

        var lastTab = tabList.stream().filter(tab -> {
            var t = (GroupSharedSubTab) tab.getUserData();
            return t.groupName.equals(this.lastSelectedGroupName);
        }).findFirst().orElse(null);

        this.tabPane.getTabs().setAll(tabList);
        if (lastTab != null) {
            this.tabPane.getSelectionModel().select(lastTab);
        }
    }

}
