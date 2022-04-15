package info.skyblond.archivedag.apwiho.scenes.file.details;

import info.skyblond.archivedag.apwiho.interfaces.BasicScene;
import info.skyblond.archivedag.apwiho.services.DialogService;
import info.skyblond.archivedag.apwiho.services.GrpcClientService;
import info.skyblond.archivedag.apwiho.services.TransferService;
import info.skyblond.archivedag.ariteg.protos.AritegCommitObject;
import info.skyblond.archivedag.ariteg.protos.AritegLink;
import info.skyblond.archivedag.ariteg.protos.AritegObjectType;
import info.skyblond.archivedag.arudaz.protos.record.QueryFileRecordRequest;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.sql.Timestamp;
import java.util.LinkedList;
import java.util.concurrent.ExecutionException;

public class FileRecordDetailHistorySubTab extends BasicScene {
    private final String recordUUID;
    private final Logger logger = LoggerFactory.getLogger("FileRecordDetail");

    public FileRecordDetailHistorySubTab(String recordUUID) {
        this.recordUUID = recordUUID;
    }

    public static class TreeViewNode {
        final String name;
        final AritegObjectType type;
        final String receipt;

        public TreeViewNode(String name) {
            this(name, null, null);
        }

        public TreeViewNode(String name, AritegObjectType type, String receipt) {
            this.name = name;
            this.type = type;
            this.receipt = receipt;
        }

        @Override
        public String toString() {
            return this.name;
        }
    }

    private Button goChildCommit;
    private TreeView<TreeViewNode> treeView;
    private boolean loading;
    private final LinkedList<String> childrenReceiptStack = new LinkedList<>();
    private final StringProperty fatherCommitReceipt = new SimpleStringProperty();

    @Override
    protected @NotNull Parent generateLayout() {
        VBox root = new VBox();

        HBox buttons = new HBox(10);
        buttons.setAlignment(Pos.CENTER);
        root.getChildren().add(buttons);

        // father commit are old
        Button goParentCommit = new Button("Early<<-");
        goParentCommit.disableProperty().bind(this.fatherCommitReceipt.isEmpty());
        goParentCommit.setOnAction(e -> {
            if (!this.loading) {
                this.loading = true;
                this.childrenReceiptStack.push(this.currentCommitReceipt);
                this.currentCommitReceipt = this.fatherCommitReceipt.get();
                this.refreshLayout();
                this.loading = false;
            }
        });
        buttons.getChildren().add(goParentCommit);

        // child commit is more recent
        this.goChildCommit = new Button("->>Recent");
        this.goChildCommit.setOnAction(e -> {
            if (!this.loading && !this.childrenReceiptStack.isEmpty()) {
                this.loading = true;
                this.fatherCommitReceipt.set(this.currentCommitReceipt);
                this.currentCommitReceipt = this.childrenReceiptStack.pop();
                this.refreshLayout();
                this.loading = false;
            }
        });
        buttons.getChildren().add(this.goChildCommit);

        HBox container = new HBox();
        VBox.setVgrow(container, Priority.ALWAYS);
        root.getChildren().add(container);
        // wrap into HBox so we have horizontal scrollbar
        this.treeView = new TreeView<>();
        HBox.setHgrow(this.treeView, Priority.ALWAYS);
        container.getChildren().add(this.treeView);
        this.treeView.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.SECONDARY) {
                TreeItem<TreeViewNode> item = this.treeView.getSelectionModel().getSelectedItem();
                this.download(e, item.getValue());
            }
        });

        this.loading = false;

        return root;
    }

    private void download(MouseEvent e, TreeViewNode node) {
        this.logger.info("Download " + node.name);
        if (node.type == null) {
            return;
        }
        Node n = (Node) e.getSource();
        Stage s = (Stage) n.getScene().getWindow();
        DirectoryChooser dirChooser = new DirectoryChooser();
        dirChooser.setTitle("Choose a folder to save");
        File folder = dirChooser.showDialog(s);
        if (folder == null) {
            return;
        }
        var a = DialogService.getInstance().showWaitingDialog("Downloading...");
        try {
            switch (node.type) {
                case BLOB ->
                        TransferService.getInstance().downloadBlobIntoFile(node.receipt, new File(folder, node.name));
                case LIST ->
                        TransferService.getInstance().downloadListIntoFile(node.receipt, new File(folder, node.name));
                case TREE ->
                        TransferService.getInstance().downloadTreeIntoFolder(node.receipt, new File(folder, node.name));
            }
            DialogService.getInstance().showInfoDialog("Download finish", node.name + " downloaded", "Success");
        } catch (Throwable t) {
            t.printStackTrace();
            DialogService.getInstance().showExceptionDialog(t, "Failed to download");
        } finally {
            a.close();
        }

    }

    private String currentCommitReceipt = null;

    private String getRootReceipt() throws ExecutionException, InterruptedException {
        return GrpcClientService.getInstance().getFileRecordServiceFutureStub().queryFileRecord(QueryFileRecordRequest.newBuilder()
                .setRecordUuid(this.recordUUID)
                .build()).get().getReceipt();
    }

    private AritegCommitObject readCommit(String receipt) throws ExecutionException, InterruptedException {
        return TransferService.getInstance().downloadCommit(receipt);
    }

    private String resolveAuthor(AritegLink link) throws ExecutionException, InterruptedException {
        return TransferService.getInstance().downloadBlob(
                TransferService.getInstance().parseReceipt(link)
        ).getData().toStringUtf8();
    }

    private String resolveTimestamp(long timestamp) {
        return new Timestamp(timestamp * 1000).toString();
    }

    private void resolveItem(TreeItem<TreeViewNode> root, AritegLink link, String name) throws ExecutionException, InterruptedException {
        root.setExpanded(false);
        String receipt = TransferService.getInstance().parseReceipt(link);
        TreeItem<TreeViewNode> node = new TreeItem<>(new TreeViewNode(name, link.getType(), receipt));
        if (link.getType() == AritegObjectType.TREE) {
            var treeObj = TransferService.getInstance().downloadTree(receipt);
            for (AritegLink treeLink : treeObj.getLinksList()) {
                this.resolveItem(node, treeLink, treeLink.getName());
            }
        }
        root.getChildren().add(node);
    }

    @Override
    protected void refreshLayout() {
        this.goChildCommit.setDisable(this.childrenReceiptStack.isEmpty());
        var alert = DialogService.getInstance().showWaitingDialog("Requesting info...");
        try {
            if (this.currentCommitReceipt == null || this.currentCommitReceipt.isBlank()) {
                this.currentCommitReceipt = this.getRootReceipt();
            }
            TreeItem<TreeViewNode> root;
            if (this.currentCommitReceipt.isBlank()) {
                root = new TreeItem<>(new TreeViewNode("No history"));
            } else {
                var commit = this.readCommit(this.currentCommitReceipt);
                this.fatherCommitReceipt.set(TransferService.getInstance().parseReceipt(commit.getParent()));

                root = new TreeItem<>(new TreeViewNode(this.resolveAuthor(commit.getAuthor())
                        + "@" + this.resolveTimestamp(commit.getUnixTimestamp())
                        + ", " + commit.getMessage()));
                this.resolveItem(root, commit.getCommittedObject(), this.recordUUID);
            }
            this.treeView.setRoot(root);
        } catch (Throwable t) {
            t.printStackTrace();
            DialogService.getInstance().showExceptionDialog(t, "Failed to fetch history.");
        }
        alert.close();
    }
}
