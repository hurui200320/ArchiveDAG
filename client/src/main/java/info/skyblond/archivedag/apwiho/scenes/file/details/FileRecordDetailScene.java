package info.skyblond.archivedag.apwiho.scenes.file.details;

import info.skyblond.archivedag.apwiho.scenes.file.FileRecordManagementScene;
import info.skyblond.archivedag.apwiho.scenes.templates.TabPage;
import javafx.scene.Scene;

public class FileRecordDetailScene extends TabPage {
    private final String recordUUID;

    public FileRecordDetailScene(String recordUUID, Scene currentScene, FileRecordManagementScene fatherScene) {
        super(currentScene, fatherScene);
        this.recordUUID = recordUUID;
        // TODO: Basics(details + {update, transfer, etc.}), Details, history, rules
        this.initialTabs.add(new TabDescriptor("Basic", false,
                new FileRecordDetailBasicSubTab(recordUUID, t -> this.goBack())));
        this.initialTabs.add(new TabDescriptor(
                "History", false,
                new FileRecordDetailHistorySubTab(recordUUID)));
        this.initialTabs.add(new TabDescriptor(
                "Rules", false,
                new FileRecordDetailRulesSubTab(recordUUID)));
    }

    @Override
    protected String getPagePath() {
        return "Home > File record management > " + this.recordUUID;
    }
}
