package info.skyblond.archivedag.apwiho.scenes.templates;

import info.skyblond.archivedag.apwiho.interfaces.CanGoBackScene;
import info.skyblond.archivedag.apwiho.interfaces.Renderable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.text.Font;
import org.jetbrains.annotations.NotNull;

/**
 * Basic framework for menu and pages. The framework would be:
 * <p>
 * <p>
 * ┌──────────────────────────────────────────┐
 * │ ┌──────────────────────────────────────┐ │
 * │ │ PagePathLabel           GoBackButton │ │
 * │ └──────────────────────────────────────┘ │
 * │ ┌──────────────────────────────────────┐ │
 * │ │                                      │ │
 * │ │            Page content              │ │
 * │ │                                      │ │
 * │ └──────────────────────────────────────┘ │
 * └──────────────────────────────────────────┘
 */
public abstract class BasicPage extends CanGoBackScene {
    public BasicPage(Scene currentScene, Renderable goBackScene) {
        super(currentScene, goBackScene);
    }

    /**
     * Content for page path label.
     */
    protected abstract String getPagePath();

    /**
     * Page content layout.
     */
    protected abstract Parent generateBorderCenter();

    /**
     * Refresh page content.
     */
    protected abstract void refreshBorderCenter();

    private Label pagePathLabel;

    @Override
    protected @NotNull Parent generateLayout() {
        BorderPane root = new BorderPane();
        HBox top = new HBox();
        top.setAlignment(Pos.CENTER_LEFT);
        root.setTop(top);

        this.pagePathLabel = new Label(this.getPagePath());
        this.pagePathLabel.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(this.pagePathLabel, Priority.ALWAYS);
        HBox.setMargin(this.pagePathLabel, new Insets(10, 10, 10, 10));
        this.pagePathLabel.setFont(Font.font(18));
        top.getChildren().add(this.pagePathLabel);

        Button goBackButton = new Button("Go Back");
        goBackButton.setFont(Font.font(14));
        goBackButton.setOnAction(e -> this.goBack());
        HBox.setHgrow(goBackButton, Priority.NEVER);
        HBox.setMargin(goBackButton, new Insets(10, 10, 10, 10));
        top.getChildren().add(goBackButton);

        root.setCenter(this.generateBorderCenter());

        return root;
    }

    @Override
    protected void refreshLayout() {
        this.pagePathLabel.setText(this.getPagePath());
        this.refreshBorderCenter();
    }
}
