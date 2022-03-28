package info.skyblond.archivedag.apwiho.scenes.group;

import info.skyblond.archivedag.apwiho.scenes.HomeScene;
import info.skyblond.archivedag.apwiho.scenes.group.tabs.*;
import info.skyblond.archivedag.apwiho.scenes.templates.TabPage;
import info.skyblond.archivedag.apwiho.services.DialogService;
import javafx.scene.Scene;

public class GroupManagementScene extends TabPage {

    public GroupManagementScene(Scene currentScene, HomeScene goBackScene) {
        super(currentScene, goBackScene);
        this.initialTabs.add(new TabDescriptor("Search", false, new SearchGroupTab(this::appendNewClosableTab, this::closeTabByGroupName)));
        this.initialTabs.add(new TabDescriptor("Owned", false, new OwnedGroupTab(this::appendNewClosableTab, this::closeTabByGroupName)));
        this.initialTabs.add(new TabDescriptor("Joined", false, new JoinedGroupTab(this::appendNewClosableTab, this::closeTabByGroupName)));
        this.initialTabs.add(new TabDescriptor("Create", false, new CreateGroupTab(this::appendNewClosableTab, this::closeTabByGroupName)));
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

    private void closeTabByGroupName(String groupName) {
        this.closeTab(t -> {
            if (t.getUserData() instanceof GroupDetailTab detailTab) {
                return detailTab.groupName.equals(groupName);
            } else {
                return false;
            }
        });
    }
}
