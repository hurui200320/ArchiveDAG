package info.skyblond.archivedag.apwiho.scenes.templates;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.TableView;

import java.util.List;
import java.util.function.Consumer;

public class FiniteTableView<S> {
    public final TableView<S> tableView;
    public final int maxSize;
    public final SimpleBooleanProperty isTooManyRecords;

    public FiniteTableView(
            int maxSize,
            Consumer<TableView<S>> tableViewConfigurator
    ) {
        this.isTooManyRecords = new SimpleBooleanProperty(false);
        this.tableView = new TableView<>();
        this.maxSize = maxSize;
        tableViewConfigurator.accept(this.tableView);
    }

    public void setContent(List<S> newContent) {
        this.isTooManyRecords.set(newContent.size() >= this.maxSize);
        this.tableView.getItems().setAll(newContent.subList(0, Math.min(newContent.size(), this.maxSize)));
    }
}
