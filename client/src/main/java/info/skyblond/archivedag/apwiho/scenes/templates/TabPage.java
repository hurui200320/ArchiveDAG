package info.skyblond.archivedag.apwiho.scenes.templates;

import info.skyblond.archivedag.apwiho.interfaces.Renderable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * An implementation of {@link BasicPage} where the page content is
 * a {@link TabPane}.
 */
public abstract class TabPage extends BasicPage {

    public record TabDescriptor(String tabTitle, boolean canClose, Renderable tabContent) {
    }

    /**
     * Initial tabs. Only used when generating border center.
     */
    protected final List<TabDescriptor> initialTabs;

    public TabPage(Scene currentScene, Renderable goBackScene) {
        super(currentScene, goBackScene);
        this.initialTabs = new LinkedList<>();
    }

    private TabPane rootTabPane;

    @Override
    protected Parent generateBorderCenter() {
        this.rootTabPane = new TabPane();
        // allow use all spaces
        this.rootTabPane.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        // cannot reorder tabs
        this.rootTabPane.setTabDragPolicy(TabPane.TabDragPolicy.FIXED);

        // render tabs
        this.initialTabs.forEach(t -> {
            Tab tab = new Tab(t.tabTitle);
            tab.setUserData(t.tabContent); // Save the renderable to tab
            tab.setClosable(t.canClose);
            tab.setContent(t.tabContent.renderRoot());
            this.rootTabPane.getTabs().add(tab);
        });

        // refresh tab when selected
        this.rootTabPane.getSelectionModel().selectedItemProperty()
                .addListener((observable, oldValue, newTab) -> {
                    var renderable = (Renderable) newTab.getUserData();
                    // refresh the new tab by render it again
                    newTab.setContent(renderable.renderRoot());
                });
        return this.rootTabPane;
    }

    protected void appendNewClosableTab(String tabTitle, Renderable content) {
        Tab tab = new Tab(tabTitle);
        tab.setUserData(content); // Save the renderable to tab
        tab.setClosable(true);
        tab.setContent(content.renderRoot());
        Objects.requireNonNull(this.rootTabPane, "Uninitialized use")
                .getTabs().add(tab);
        this.rootTabPane.getSelectionModel().select(tab);
    }

    protected void closeTab(Predicate<Tab> predicate) {
        var result = this.rootTabPane.getTabs().stream().filter(predicate).findFirst();
        result.ifPresent(tab -> this.rootTabPane.getTabs().remove(tab));
    }

    @Override
    protected void refreshBorderCenter() {
        // refresh current selected tab
        if (this.rootTabPane != null) {
            Tab tab = this.rootTabPane.getSelectionModel().getSelectedItem();
            var renderable = (Renderable) tab.getUserData();
            // refresh the new tab by render it again
            tab.setContent(renderable.renderRoot());
        }
    }
}
