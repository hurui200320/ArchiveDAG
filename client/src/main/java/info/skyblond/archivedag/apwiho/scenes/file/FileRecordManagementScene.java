package info.skyblond.archivedag.apwiho.scenes.file;

import info.skyblond.archivedag.apwiho.scenes.HomeScene;
import info.skyblond.archivedag.apwiho.scenes.file.tabs.*;
import info.skyblond.archivedag.apwiho.scenes.templates.TabPage;
import javafx.scene.Scene;

public class FileRecordManagementScene extends TabPage {

    public FileRecordManagementScene(Scene currentScene, HomeScene goBackScene) {
        super(currentScene, goBackScene);
        this.initialTabs.add(new TabDescriptor("Owned", false, new OwnedRecordTab(this.getCurrentScene(), this)));
        this.initialTabs.add(new TabDescriptor("User share", false, new UserSharedRecordTab(this.getCurrentScene(), this)));
        this.initialTabs.add(new TabDescriptor("Group share", false, new GroupSharedRecordTab(this.getCurrentScene(), this)));
        this.initialTabs.add(new TabDescriptor("Public share", false, new PublicSharedRecordTab(this.getCurrentScene(), this)));
        this.initialTabs.add(new TabDescriptor("Create", false, new CreateFileRecordTab(this.getCurrentScene(), this)));
    }

    @Override
    protected String getPagePath() {
        return "Home > File record management";
    }


}
