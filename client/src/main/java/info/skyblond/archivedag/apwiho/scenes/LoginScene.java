package info.skyblond.archivedag.apwiho.scenes;

import info.skyblond.archivedag.apwiho.interfaces.SwappableScene;
import info.skyblond.archivedag.apwiho.services.DialogService;
import info.skyblond.archivedag.apwiho.services.GrpcClientService;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class LoginScene extends SwappableScene {

    public LoginScene(Scene currentScene) {
        super(currentScene);
    }

    private File serverCA;
    private File userCert;
    private File userPrivateKey;

    private void refreshConnectButtonDisabledProperty() {
        var hostTextNotEmpty = !this.hostTextInput.getText().isBlank();
        var allowConnect = hostTextNotEmpty && this.serverCA != null && this.userCert != null && this.userPrivateKey != null;
        this.connectButton.setDisable(!allowConnect);
    }

    private void chooseFile(String title, ActionEvent e, Consumer<File> callback) {
        Node n = (Node) e.getSource();
        Stage s = (Stage) n.getScene().getWindow();

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(title);
        fileChooser.setInitialDirectory(new File("."));
        fileChooser.getExtensionFilters()
                .add(new FileChooser.ExtensionFilter("X509 PEM", "*.pem"));
        File file = fileChooser.showOpenDialog(s);
        callback.accept(file);

        this.refreshConnectButtonDisabledProperty();
    }

    private TextField hostTextInput;
    private Button connectButton;

    private void generateFormPart(VBox form, String title, Supplier<Node> childGenerator) {
        Label titleLabel = new Label(title);
        VBox.setMargin(titleLabel, new Insets(10, 10, 10, 10));
        form.getChildren().add(titleLabel);
        Node child = childGenerator.get();
        form.getChildren().add(child);
    }

    private HBox generateFileSelectionPart(String chooseFileTitle, Consumer<File> preserveFileCallback) {
        HBox fileSelection = new HBox();
        fileSelection.setAlignment(Pos.CENTER_LEFT);
        VBox.setMargin(fileSelection, new Insets(0, 10, 10, 10));
        Button selectFileButton = new Button("Choose file...");
        HBox.setMargin(selectFileButton, new Insets(0, 10, 0, 0));
        fileSelection.getChildren().add(selectFileButton);
        Label selectedLabel = new Label();
        fileSelection.getChildren().add(selectedLabel);

        // register the onAction handler
        selectFileButton.setOnAction(e -> this.chooseFile(chooseFileTitle, e, file -> {
            // save file to the field
            preserveFileCallback.accept(file);
            // update label
            if (file != null) {
                selectedLabel.setText(file.getName());
            } else {
                selectedLabel.setText("");
            }
        }));
        // return generated HBox
        return fileSelection;
    }

    @Override
    protected @NotNull Parent generateLayout() {
        BorderPane root = new BorderPane();
        HBox center = new HBox();
        center.setAlignment(Pos.CENTER);
        root.setCenter(center);
        VBox form = new VBox();
        form.setAlignment(Pos.CENTER_LEFT);
        form.setPrefWidth(350);
        center.getChildren().add(form);

        this.generateFormPart(form, "Host:", () -> {
            this.hostTextInput = new TextField("127.0.0.1:9090");
            this.hostTextInput.textProperty().addListener((observable, oldValue, newValue) -> {
                if (!oldValue.equals(newValue)) {
                    this.refreshConnectButtonDisabledProperty();
                }
            });
            VBox.setMargin(this.hostTextInput, new Insets(0, 10, 10, 10));
            return this.hostTextInput;
        });

        this.generateFormPart(form, "Server CA file:", () -> this.generateFileSelectionPart(
                "Select server CA file",
                file -> this.serverCA = file
        ));

        this.generateFormPart(form, "Certification file:", () -> this.generateFileSelectionPart(
                "Select user cert file",
                file -> this.userCert = file
        ));

        this.generateFormPart(form, "Private key file:", () -> this.generateFileSelectionPart(
                "Select user private key file",
                file -> this.userPrivateKey = file
        ));

        HBox connectButtonContainer = new HBox();
        connectButtonContainer.setAlignment(Pos.CENTER);
        VBox.setMargin(connectButtonContainer, new Insets(30, 0, 0, 0));
        form.getChildren().add(connectButtonContainer);
        this.connectButton = new Button("Connect...");
        this.connectButton.setOnAction(e -> this.doConnect());
        connectButtonContainer.getChildren().add(this.connectButton);

        return root;
    }

    @Override
    protected void refreshLayout() {
        // no need to refresh
    }

    private void doConnect() {
        this.connectButton.setDisable(true);
        var grpcService = GrpcClientService.getInstance();
        try {
            grpcService.init(this.serverCA, this.userCert, this.userPrivateKey, this.hostTextInput.getText());
            this.swapToHomePage();
        } catch (Throwable t) {
            DialogService.getInstance().showExceptionDialog(t, "Failed to initialize the connection to server");
        } finally {
            this.refreshConnectButtonDisabledProperty();
        }
    }

    private void swapToHomePage() {
        HomeScene homeScene = new HomeScene(this.getCurrentScene());
        this.swapTo(homeScene);
    }
}
