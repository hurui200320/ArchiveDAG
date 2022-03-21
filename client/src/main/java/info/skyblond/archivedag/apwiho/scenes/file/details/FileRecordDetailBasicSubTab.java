package info.skyblond.archivedag.apwiho.scenes.file.details;

import info.skyblond.archivedag.apwiho.interfaces.BasicScene;
import info.skyblond.archivedag.apwiho.services.DialogService;
import info.skyblond.archivedag.apwiho.services.GrpcClientService;
import info.skyblond.archivedag.arudaz.protos.record.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.jetbrains.annotations.NotNull;

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

        Button update = new Button("New version");
        buttons.getChildren().add(update);
        update.setPrefWidth(125);
        update.setOnAction(event -> this.newVersion());

        Button rename = new Button("Rename record");
        buttons.getChildren().add(rename);
        rename.setPrefWidth(125);
        rename.setOnAction(event -> this.renameRecord());

        Button transfer = new Button("Transfer record");
        buttons.getChildren().add(transfer);
        transfer.setPrefWidth(125);
        transfer.setOnAction(event -> this.transferRecord());

        Button delete = new Button("Delete record");
        buttons.getChildren().add(delete);
        delete.setPrefWidth(125);
        delete.setOnAction(event -> this.deleteRecord());

        return root;
    }

    private void newVersion() {
        var receipt = DialogService.getInstance().showTextInputDialog("New version", "What's the new version?", "Receipt:", "");
        if (receipt == null) {
            return;
        }
        var message = DialogService.getInstance().showTextInputDialog("New version", "What's the commit message?", "Message:", "");
        if (message == null) {
            return;
        }
        try {
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
