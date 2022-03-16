package info.skyblond.archivedag.apwiho.scenes.group;

import info.skyblond.archivedag.apwiho.scenes.HomeScene;
import info.skyblond.archivedag.apwiho.scenes.group.tabs.CreateGroupTab;
import info.skyblond.archivedag.apwiho.scenes.group.tabs.JoinedGroupTab;
import info.skyblond.archivedag.apwiho.scenes.group.tabs.OwnedGroupTab;
import info.skyblond.archivedag.apwiho.scenes.group.tabs.SearchGroupTab;
import info.skyblond.archivedag.apwiho.scenes.templates.TabPage;
import info.skyblond.archivedag.apwiho.services.DialogService;
import javafx.scene.Scene;

public class GroupManagementScene extends TabPage {

    public GroupManagementScene(Scene currentScene, HomeScene goBackScene) {
        super(currentScene, goBackScene);
        this.initialTabs.add(new TabDescriptor("Search", false, new SearchGroupTab(this::appendNewClosableTab)));
        this.initialTabs.add(new TabDescriptor("Owned", false, new OwnedGroupTab(this::appendNewClosableTab)));
        this.initialTabs.add(new TabDescriptor("Joined", false, new JoinedGroupTab(this::appendNewClosableTab)));
        this.initialTabs.add(new TabDescriptor("Create", false, new CreateGroupTab(this::appendNewClosableTab)));
    }

    @Override
    protected String getPagePath() {
        return "Home > Group management";
    }

    @Override
    protected void refreshBorderCenter() {
        var alert = DialogService.getInstance().showWaitingDialog("Requesting info...");
        super.refreshBorderCenter();
        alert.close();
    }
}
