package info.skyblond.archivedag.apwiho.scenes.file.details;

import info.skyblond.archivedag.apwiho.interfaces.BasicScene;
import info.skyblond.archivedag.apwiho.services.DialogService;
import info.skyblond.archivedag.apwiho.services.GrpcClientService;
import info.skyblond.archivedag.apwiho.services.TransferService;
import info.skyblond.archivedag.arudaz.protos.record.*;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.sql.Timestamp;
import java.util.function.Consumer;

public class FileRecordDetailBasicSubTab extends BasicScene {
    private final String recordUUID;

    private final Consumer<FileRecordDetailBasicSubTab> goBack;

    public FileRecordDetailBasicSubTab(String recordUUID, Consumer<FileRecordDetailBasicSubTab> goBack) {
        this.recordUUID = recordUUID;
        this.goBack = goBack;
    }

    private Label infoLabel;

    @Override
    protected @NotNull Parent generateLayout() {
        HBox root = new HBox(20);
        root.setAlignment(Pos.CENTER);

        this.infoLabel = new Label();
        root.getChildren().add(this.infoLabel);
        HBox.setMargin(this.infoLabel, new Insets(0, 20, 0, 20));

        VBox buttons = new VBox(10);
        root.getChildren().add(buttons);
        buttons.setAlignment(Pos.CENTER);
        HBox.setMargin(buttons, new Insets(0, 20, 0, 20));

        Button updateFile = new Button("New version (File)");
        buttons.getChildren().add(updateFile);
        updateFile.setPrefWidth(150);
        updateFile.setOnAction(this::newVersionFile);


        Button updateDir = new Button("New version (Folder)");
        buttons.getChildren().add(updateDir);
        updateDir.setPrefWidth(150);
        updateDir.setOnAction(this::newVersionDir);

        Button rename = new Button("Rename record");
        buttons.getChildren().add(rename);
        rename.setPrefWidth(150);
        rename.setOnAction(event -> this.renameRecord());

        Button transfer = new Button("Transfer record");
        buttons.getChildren().add(transfer);
        transfer.setPrefWidth(150);
        transfer.setOnAction(event -> this.transferRecord());

        Button delete = new Button("Delete record");
        buttons.getChildren().add(delete);
        delete.setPrefWidth(150);
        delete.setOnAction(event -> this.deleteRecord());

        return root;
    }

    private void newVersionFile(ActionEvent e) {
        var message = DialogService.getInstance().showTextInputDialog("New version", "What's the commit message?", "Message:", "");
        if (message == null) {
            return;
        }
        Node n = (Node) e.getSource();
        Stage s = (Stage) n.getScene().getWindow();

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choose a file to upload");
        File file = fileChooser.showOpenDialog(s);
        if (file == null) {
            return;
        }
        var a = DialogService.getInstance().showWaitingDialog("Creating new version...");
        try {
            TransferService.getInstance().queryServerProtoConfig();
            var result = TransferService.getInstance().wrapIntoTree(this.recordUUID,
                    TransferService.getInstance().sliceAndUploadFile(this.recordUUID, file));
            var receipt = TransferService.getInstance().parseReceipt(result);

            GrpcClientService.getInstance().getFileRecordServiceFutureStub()
                    .updateFileRecordRef(UpdateFileRecordRefRequest.newBuilder()
                            .setRecordUuid(this.recordUUID)
                            .setReceipt(receipt)
                            .setMessage(message)
                            .build()).get();
            DialogService.getInstance().showInfoDialog("New version created", "New version created", "Success");
            this.refreshLayout();
        } catch (Throwable t) {
            DialogService.getInstance().showExceptionDialog(t, "Failed to create new version");
        } finally {
            a.close();
        }
    }

    private void newVersionDir(ActionEvent e) {
        var message = DialogService.getInstance().showTextInputDialog("New version", "What's the commit message?", "Message:", "");
        if (message == null) {
            return;
        }
        Node n = (Node) e.getSource();
        Stage s = (Stage) n.getScene().getWindow();

        DirectoryChooser dirChooser = new DirectoryChooser();
        dirChooser.setTitle("Choose a file to upload");
        File file = dirChooser.showDialog(s);
        if (file == null) {
            return;
        }
        var a = DialogService.getInstance().showWaitingDialog("Creating new version...");
        try {
            TransferService.getInstance().queryServerProtoConfig();
            var result = TransferService.getInstance().sliceAndUploadFolder(this.recordUUID, file);
            var receipt = TransferService.getInstance().parseReceipt(result);

            GrpcClientService.getInstance().getFileRecordServiceFutureStub()
                    .updateFileRecordRef(UpdateFileRecordRefRequest.newBuilder()
                            .setRecordUuid(this.recordUUID)
                            .setReceipt(receipt)
                            .setMessage(message)
                            .build()).get();
            DialogService.getInstance().showInfoDialog("New version created", "New version created", "Success");
            this.refreshLayout();
        } catch (Throwable t) {
            t.printStackTrace();
            DialogService.getInstance().showExceptionDialog(t, "Failed to create new version");
        } finally {
            a.close();
        }
    }

    private void renameRecord() {
        var newName = DialogService.getInstance().showTextInputDialog("Rename record", "What's the new name?", "New name:", "");
        if (newName == null) {
            return;
        }
        try {
            GrpcClientService.getInstance().getFileRecordServiceFutureStub()
                    .updateFileRecordName(UpdateFileRecordNameRequest.newBuilder()
                            .setRecordUuid(this.recordUUID)
                            .setNewRecordName(newName)
                            .build()).get();
            DialogService.getInstance().showInfoDialog("Renamed to " + newName + " .", "Record renamed", "Success");
            this.refreshLayout();
        } catch (Throwable t) {
            DialogService.getInstance().showExceptionDialog(t, "Failed to rename record");
        }
    }

    private void transferRecord() {
        var targetUsername = DialogService.getInstance().showTextInputDialog("Transfer record", "To which user?", "Username:", "");
        if (targetUsername == null) {
            return;
        }
        try {
            GrpcClientService.getInstance().getFileRecordServiceFutureStub()
                    .transferFileRecord(TransferFileRecordRequest.newBuilder()
                            .setRecordUuid(this.recordUUID)
                            .setNewOwner(targetUsername)
                            .build()).get();
            DialogService.getInstance().showInfoDialog("Transferred to " + targetUsername + " .", "Record transferred", "Success");
            this.refreshLayout();
        } catch (Throwable t) {
            DialogService.getInstance().showExceptionDialog(t, "Failed to transfer record");
        }
    }

    private void deleteRecord() {
        if (!DialogService.getInstance().showYesOrNoDialog("Delete record", "Delete " + this.recordUUID, "Really?")) {
            return;
        }
        try {
            GrpcClientService.getInstance().getFileRecordServiceFutureStub()
                    .deleteFileRecord(DeleteFileRecordRequest.newBuilder()
                            .setRecordUuid(this.recordUUID)
                            .build()).get();
            DialogService.getInstance().showInfoDialog("Record " + this.recordUUID + " has been deleted.", "Record deleted", "Success");
            this.goBack.accept(this);
        } catch (Throwable t) {
            DialogService.getInstance().showExceptionDialog(t, "Failed to delete record");
        }
    }

    @Override
    protected void refreshLayout() {
        var alert = DialogService.getInstance().showWaitingDialog("Requesting info...");
        this.updateInfoLabel();
        alert.close();
    }

    private void updateInfoLabel() {
        try {
            var basicResult = GrpcClientService.getInstance().getFileRecordServiceFutureStub()
                    .queryFileRecord(QueryFileRecordRequest.newBuilder()
                            .setRecordUuid(this.recordUUID)
                            .build()).get();
            var sb = new StringBuilder();
            sb.append("Record uuid: ").append(basicResult.getRecordUuid()).append("\n\n");
            sb.append("Record name: ").append(basicResult.getRecordName()).append("\n\n");
            sb.append("Created at: ").append(new Timestamp(basicResult.getCreatedTimestamp() * 1000)).append("\n\n");
            sb.append("Owner: ").append(basicResult.getOwner()).append("\n\n\n");

            var permissionResult = GrpcClientService.getInstance().getFileRecordServiceFutureStub()
                    .queryMyPermissionOnRecord(QueryMyPermissionOnRecordRequest.newBuilder()
                            .setRecordUuid(this.recordUUID)
                            .build()).get();
            sb.append("My permission: ").append("(").append(permissionResult.getPermission()).append(")").append("\n");
            for (char c : permissionResult.getPermission().toCharArray()) {
                switch (c) {
                    case 'r' -> sb.append("  + r: read").append("\n");
                    case 'u' -> sb.append("  + u: create new version").append("\n");
                    case 'n' -> sb.append("  + n: rename").append("\n");
                    default -> sb.append("  + ").append(c).append(": unknown").append("\n");
                }
            }

            if (this.infoLabel != null) {
                this.infoLabel.setText(sb.toString());
            }
        } catch (Throwable t) {
            DialogService.getInstance().showExceptionDialog(t, "Failed to fetch record info.");
            if (this.infoLabel != null) {
                this.infoLabel.setText("Failed to fetch info");
            }
            this.goBack.accept(this);
        }
    }
}
