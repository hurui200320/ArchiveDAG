package info.skyblond.archivedag.apwiho.services;

import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;

import java.util.function.Consumer;

public class JavaFXUtils {
    public static <S> void setTableViewDoubleAction(TableView<S> tableView, Consumer<S> callback) {
        tableView.setRowFactory(t -> {
            TableRow<S> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    callback.accept(row.getItem());
                }
            });
            return row;
        });
    }
}
