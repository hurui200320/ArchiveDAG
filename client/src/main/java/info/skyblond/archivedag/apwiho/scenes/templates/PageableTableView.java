package info.skyblond.archivedag.apwiho.scenes.templates;

import info.skyblond.archivedag.apwiho.interfaces.BasicScene;
import info.skyblond.archivedag.arudaz.protos.common.Page;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public class PageableTableView<S> extends BasicScene {

    private final int size;
    private int page;
    private final int pageOffset;
    private final Function<Page, List<S>> contentLoader;
    private final Consumer<TableView<S>> tableViewConfigurator;

    public PageableTableView(
            int size, int pageOffset,
            Function<Page, List<S>> contentLoader,
            Consumer<TableView<S>> tableViewConfigurator
    ) {
        this.size = size;
        this.page = 1;
        this.pageOffset = pageOffset;
        this.contentLoader = contentLoader;
        this.tableViewConfigurator = tableViewConfigurator;
    }

    private Button prevPage;
    private Label currentPage;
    private Button nextPage;
    private TableView<S> tableView;
    private volatile boolean loading;

    @Override
    protected @NotNull Parent generateLayout() {
        VBox root = new VBox();

        HBox buttons = new HBox(10);
        buttons.setAlignment(Pos.CENTER);
        root.getChildren().add(buttons);

        this.prevPage = new Button("Prev");
        this.prevPage.setOnAction(e -> {
            if (!this.loading) {
                this.page--;
                this.refreshLayout();
            }
        });
        buttons.getChildren().add(this.prevPage);

        this.currentPage = new Label();
        buttons.getChildren().add(this.currentPage);

        this.nextPage = new Button("Next");
        this.nextPage.setOnAction(e -> {
            if (!this.loading) {
                this.page++;
                this.refreshLayout();
            }
        });
        buttons.getChildren().add(this.nextPage);

        this.tableView = new TableView<>();
        this.tableViewConfigurator.accept(this.tableView);
        VBox.setVgrow(this.tableView, Priority.ALWAYS);
        root.getChildren().add(this.tableView);

        this.loading = false;

        return root;
    }

    @Override
    protected void refreshLayout() {
        this.loading = true;
        if (this.page < 1) {
            this.page = 1;
        }

        var pageProto = Page.newBuilder()
                .setPage(this.page + this.pageOffset)
                .setSize(this.size)
                .build();
        var result = this.contentLoader.apply(pageProto);

        this.prevPage.setDisable(this.page <= 1);
        this.currentPage.setText(String.valueOf(this.page));
        this.nextPage.setDisable(result.size() < this.size);
        this.tableView.getItems().setAll(result);
        this.loading = false;
    }
}
