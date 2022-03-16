package info.skyblond.archivedag.apwiho.scenes.group;

import info.skyblond.archivedag.apwiho.interfaces.Renderable;
import info.skyblond.archivedag.apwiho.scenes.templates.TabPage;
import javafx.scene.Scene;

public class GroupManagementScene extends TabPage {

    public GroupManagementScene(Scene currentScene, Renderable goBackScene) {
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
}
