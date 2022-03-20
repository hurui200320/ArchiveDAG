package info.skyblond.archivedag.apwiho.scenes.file;

import info.skyblond.archivedag.apwiho.scenes.templates.TabPage;
import javafx.scene.Scene;
import javafx.scene.control.Label;

public class FileRecordDetailScene extends TabPage {
    private final String recordUUID;

    // TODO: dialog when loading, error popup

    public FileRecordDetailScene(String recordUUID, Scene currentScene, FileRecordManagementScene fatherScene) {
        super(currentScene, fatherScene);
        this.recordUUID = recordUUID;
        // TODO: Basics(details + {update, transfer, etc.}), Details, history, rules
        this.initialTabs.add(new TabDescriptor(
                "TODO", false,
                () -> new Label("Details: " + recordUUID)));
    }

    @Override
    protected String getPagePath() {
        return "Home > File record management > " + this.recordUUID;
    }
}
