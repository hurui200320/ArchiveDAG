package info.skyblond.archivedag.apwiho.services;

import javafx.beans.property.StringProperty;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;

import java.util.function.Consumer;
import java.util.function.Function;

public class JavaFXUtils {
    public static <S> Consumer<TableRow<S>> setRowOnDoubleClick(Consumer<S> callback) {
        return row -> row.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && (!row.isEmpty())) {
                callback.accept(row.getItem());
            }
        });
    }

    public static <S> Consumer<TableRow<S>> setRowOnHighlightChange(StringProperty keywordProperty, Function<S, String> itemStringExtractor) {
        return row -> {
            keywordProperty.addListener((observable, oldValue, newValue) -> {
                if (row.getItem() == null || newValue.isBlank()) {
                    row.setStyle("");
                    return;
                }
                if (itemStringExtractor.apply(row.getItem()).contains(newValue)) {
                    row.setStyle("-fx-background-color: yellow");
                } else {
                    row.setStyle("");
                }
            });
            row.itemProperty().addListener((observable, oldValue, newValue) -> {
                var currentText = keywordProperty.get();
                if (newValue == null || currentText.isBlank()) {
                    row.setStyle("");
                    return;
                }
                if (itemStringExtractor.apply(row.getItem()).contains(currentText)) {
                    row.setStyle("-fx-background-color: yellow");
                } else {
                    row.setStyle("");
                }
            });
        };
    }

    @SafeVarargs
    public static <S> void setTableViewWithFactory(TableView<S> tableView, Consumer<TableRow<S>>... configs) {
        tableView.setRowFactory(t -> {
            TableRow<S> row = new TableRow<>();
            for (Consumer<TableRow<S>> config : configs) {
                config.accept(row);
            }
            return row;
        });
    }

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
