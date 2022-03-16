package info.skyblond.archivedag.apwiho.services;

import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public class DialogService {
    private static final DialogService ourInstance = new DialogService();

    public static DialogService getInstance() {
        return ourInstance;
    }

    private DialogService() {
    }

    public boolean showYesOrNoDialog(
            String title, String headerText, String contentText
    ) {
        var alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(headerText);
        alert.setContentText(contentText);
        alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);

        return alert.showAndWait().orElse(null) != ButtonType.CANCEL;
    }

    public String showTextInputDialog(
            String title, String headerText, String contentText, String defaultValue
    ) {
        var dialog = new TextInputDialog();
        dialog.setTitle(title);
        dialog.setHeaderText(headerText);
        dialog.setContentText(contentText);
        dialog.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);

        if (defaultValue != null) {
            dialog.getEditor().setText(defaultValue);
        }

        var result = new AtomicReference<String>(null);
        dialog.showAndWait().ifPresent(result::set);
        return result.get();
    }

    public void showInfoDialog(
            String contentText, String headerText
    ) {
        this.showInfoDialog(contentText, headerText, null);
    }

    public void showInfoDialog(
            String contentText, String headerText, String title
    ) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(Objects.requireNonNullElse(title, "Info"));
        alert.setHeaderText(Objects.requireNonNullElse(headerText, "Operation is done"));
        alert.setContentText(Objects.requireNonNullElse(contentText, "The operation is finished."));

        alert.showAndWait();
    }


    public void showExceptionDialog(Throwable t) {
        this.showExceptionDialog(t, null, null, null);
    }

    public void showExceptionDialog(Throwable t, String contentText) {
        this.showExceptionDialog(t, contentText, null, null);
    }

    public void showExceptionDialog(Throwable t, String contentText, String headerText) {
        this.showExceptionDialog(t, contentText, headerText, null);
    }

    public void showExceptionDialog(
            Throwable t, String contentText, String headerText, String title
    ) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(Objects.requireNonNullElse(title, "Error"));
        alert.setHeaderText(Objects.requireNonNullElse(headerText, "Exception occurred"));
        alert.setContentText(Objects.requireNonNullElse(contentText, "Failed to perform the requested operation."));

        // Create expandable Exception.
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        t.printStackTrace(pw);
        String exceptionText = sw.toString();

        Label label = new Label("The stacktrace was:");

        TextArea textArea = new TextArea(exceptionText);
        textArea.setEditable(false);
        textArea.setWrapText(true);

        textArea.setMaxWidth(Double.MAX_VALUE);
        textArea.setMaxHeight(Double.MAX_VALUE);
        GridPane.setVgrow(textArea, Priority.ALWAYS);
        GridPane.setHgrow(textArea, Priority.ALWAYS);

        GridPane expContent = new GridPane();
        expContent.setMaxWidth(Double.MAX_VALUE);
        expContent.add(label, 0, 0);
        expContent.add(textArea, 0, 1);

        // Set expandable Exception into the dialog pane.
        alert.getDialogPane().setExpandableContent(expContent);

        alert.showAndWait();
    }

    public Alert showWaitingDialog(String contentText) {
        return this.showWaitingDialog(contentText, null, null);
    }

    public Alert showWaitingDialog(String contentText, String headerText) {
        return this.showWaitingDialog(contentText, headerText, null);
    }

    public Alert showWaitingDialog(
            String contentText, String headerText, String title
    ) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.getButtonTypes().clear();
        // Add a hidden close button
        alert.getButtonTypes().add(ButtonType.CANCEL);
        Node closeButton = alert.getDialogPane().lookupButton(ButtonType.CANCEL);
        closeButton.managedProperty().bind(closeButton.visibleProperty());
        closeButton.setVisible(false);

        alert.setTitle(Objects.requireNonNullElse(title, "Processing..."));
        alert.setHeaderText(Objects.requireNonNullElse(headerText, "Please wait..."));
        alert.setContentText(Objects.requireNonNullElse(contentText, "Performing operation..."));
        alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        alert.show();
        return alert;
    }
}
