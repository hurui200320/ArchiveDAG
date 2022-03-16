package info.skyblond.archivedag.apwiho.scenes.templates;

import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.TableView;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Infinite scrolling version of {@link TableView}.
 */
public class InfiniteScrollingTableView<S> {
    /**
     * Delegated tableview.
     */
    public final TableView<S> tableView;
    private final ReloadCallback<S> reloadCallback;
    private final int size;

    private int page;
    private boolean isEnd;

    public interface ReloadCallback<S> {
        List<S> load(int page, int size);
    }

    public InfiniteScrollingTableView(
            int page, int size,
            Consumer<TableView<S>> tableViewConfigurer,
            ReloadCallback<S> reloadCallback
    ) {
        this.size = size;
        this.tableView = new TableView<>();
        this.reloadCallback = reloadCallback;
        tableViewConfigurer.accept(this.tableView);
        this.reset(page);
    }

    private void loadMore() {
        System.out.println("Loading more!");
        if (this.isEnd) {
            System.out.println("This is the end.");
            return;
        }
        var result = this.reloadCallback.load(this.page, this.size);
        System.out.println(result);
        if (result.size() < this.size) {
            System.out.println("No more data!");
            this.isEnd = true; // mark the ending
        }
        // save the result
        this.tableView.getItems().addAll(result);
        this.page++;
    }

    private ScrollBar getVerticalScrollbar() {
        for (Node n : this.tableView.lookupAll(".scroll-bar:vertical")) {
            if (n instanceof ScrollBar bar) {
                return bar;
            }
        }
        return null;
    }

    private void handleScroll(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
        System.out.println("Handle scroll event!");
        double value = newValue.doubleValue();
        ScrollBar bar = Objects.requireNonNull(this.getVerticalScrollbar());
        if (value == bar.getMax()) {
            this.loadMore();
        }
    }

    public void reset(int page) {
        this.tableView.getItems().clear();
        this.page = page;
        this.isEnd = false;
        this.loadMore();
    }

    public void registerScroll() {
        ScrollBar bar = this.getVerticalScrollbar();
        if (bar == null) {
            System.out.println("Delayed reg...");
            Platform.runLater(this::registerScroll);
        } else {
            System.out.println("Reg done!");
            if (!bar.isVisible()) {
                // load more to make scroll bar visible
                while (!this.isEnd) {
                    this.loadMore();
                }
            }
            bar.valueProperty().addListener(this::handleScroll);
        }
    }
}
