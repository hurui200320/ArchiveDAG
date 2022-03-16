package info.skyblond.archivedag.apwiho.scenes.group;

import info.skyblond.archivedag.apwiho.services.GrpcClientService;
import info.skyblond.archivedag.arudaz.protos.group.GroupDetailResponse;
import info.skyblond.archivedag.arudaz.protos.group.QueryGroupRequest;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.PropertyValueFactory;

import java.sql.Timestamp;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class GroupTableModel {
    public static List<GroupTableModel> resolveFromGroupNameList(List<String> groupNameList) throws ExecutionException, InterruptedException {
        var requestList = groupNameList.stream()
                .map(name -> GrpcClientService.getInstance().getGroupServiceFutureStub()
                        .queryGroup(QueryGroupRequest.newBuilder()
                                .setGroupName(name)
                                .build())).toList();
        var resultList = groupNameList.stream().map(GroupTableModel::new).toList();
        for (int i = 0; i < resultList.size(); i++) {
            var currentR = requestList.get(i).get();
            resultList.get(i).updateFromProto(currentR);
        }
        return resultList;
    }

    public static List<TableColumn<GroupTableModel, ?>> getColumns() {
        TableColumn<GroupTableModel, String> groupName = new TableColumn<>("Name");
        groupName.setCellValueFactory(new PropertyValueFactory<>("groupName"));
        groupName.setSortable(false);

        TableColumn<GroupTableModel, String> owner = new TableColumn<>("Owner");
        owner.setCellValueFactory(new PropertyValueFactory<>("owner"));
        owner.setSortable(false);

        TableColumn<GroupTableModel, String> createdTime = new TableColumn<>("Created at");
        createdTime.setCellValueFactory(new PropertyValueFactory<>("createdTime"));
        createdTime.setSortable(false);

        return List.of(groupName, owner, createdTime);
    }


    private final SimpleStringProperty groupName;
    private final SimpleStringProperty owner;
    private final SimpleStringProperty createdTime;

    public GroupTableModel(String groupName) {
        this.groupName = new SimpleStringProperty(groupName);
        this.owner = new SimpleStringProperty();
        this.createdTime = new SimpleStringProperty();
    }

    public void updateFromProto(GroupDetailResponse response) {
        this.groupName.set(response.getGroupName());
        this.owner.set(response.getOwner());
        this.createdTime.set(new Timestamp(response.getCreatedTimestamp() * 1000).toString());
    }

    public String getGroupName() {
        return this.groupName.get();
    }

    public SimpleStringProperty groupNameProperty() {
        return this.groupName;
    }

    public String getOwner() {
        return this.owner.get();
    }

    public SimpleStringProperty ownerProperty() {
        return this.owner;
    }

    public String getCreatedTime() {
        return this.createdTime.get();
    }

    public SimpleStringProperty createdTimeProperty() {
        return this.createdTime;
    }
}
