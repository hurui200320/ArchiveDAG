package info.skyblond.archivedag.apwiho.scenes.file;

import info.skyblond.archivedag.apwiho.services.GrpcClientService;
import info.skyblond.archivedag.arudaz.protos.record.FileRecordDetailResponse;
import info.skyblond.archivedag.arudaz.protos.record.QueryFileRecordRequest;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.PropertyValueFactory;

import java.sql.Timestamp;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class FileRecordTableModel {
    public static List<FileRecordTableModel> resolveFromRecordUUIDList(List<String> recordUUIDList) throws ExecutionException, InterruptedException {
        var requestList = recordUUIDList.stream()
                .map(uuid -> GrpcClientService.getInstance().getFileRecordServiceFutureStub()
                        .queryFileRecord(QueryFileRecordRequest.newBuilder()
                                .setRecordUuid(uuid)
                                .build())).toList();
        var resultList = recordUUIDList.stream().map(FileRecordTableModel::new).toList();
        for (int i = 0; i < resultList.size(); i++) {
            var currentR = requestList.get(i).get();
            resultList.get(i).updateFromProto(currentR);
        }
        return resultList;
    }

    public static List<TableColumn<FileRecordTableModel, ?>> getColumns() {
        TableColumn<FileRecordTableModel, String> recordUUID = new TableColumn<>("UUID");
        recordUUID.setCellValueFactory(new PropertyValueFactory<>("recordUUID"));
        recordUUID.setSortable(false);

        TableColumn<FileRecordTableModel, String> recordName = new TableColumn<>("Name");
        recordName.setCellValueFactory(new PropertyValueFactory<>("recordName"));
        recordName.setSortable(false);

        TableColumn<FileRecordTableModel, String> createdTime = new TableColumn<>("Created at");
        createdTime.setCellValueFactory(new PropertyValueFactory<>("createdTime"));
        createdTime.setSortable(false);

        TableColumn<FileRecordTableModel, String> owner = new TableColumn<>("Owner");
        owner.setCellValueFactory(new PropertyValueFactory<>("owner"));
        owner.setSortable(false);

        return List.of(recordUUID, recordName, createdTime, owner);
    }

    private final SimpleStringProperty recordUUID;
    private final SimpleStringProperty recordName;
    private final SimpleStringProperty createdTime;
    private final SimpleStringProperty owner;

    public FileRecordTableModel(String recordUUID) {
        this.recordUUID = new SimpleStringProperty(recordUUID);
        this.recordName = new SimpleStringProperty();
        this.createdTime = new SimpleStringProperty();
        this.owner = new SimpleStringProperty();
    }

    public void updateFromProto(FileRecordDetailResponse response) {
        this.recordUUID.set(response.getRecordUuid());
        this.recordName.set(response.getRecordName());
        this.createdTime.set(new Timestamp(response.getCreatedTimestamp() * 1000).toString());
        this.owner.set(response.getOwner());
    }

    public String getRecordUUID() {
        return this.recordUUID.get();
    }

    public SimpleStringProperty recordUUIDProperty() {
        return this.recordUUID;
    }

    public String getRecordName() {
        return this.recordName.get();
    }

    public SimpleStringProperty recordNameProperty() {
        return this.recordName;
    }

    public String getCreatedTime() {
        return this.createdTime.get();
    }

    public SimpleStringProperty createdTimeProperty() {
        return this.createdTime;
    }

    public String getOwner() {
        return this.owner.get();
    }

    public SimpleStringProperty ownerProperty() {
        return this.owner;
    }
}
