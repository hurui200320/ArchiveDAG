package info.skyblond.archivedag.model.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.sql.Timestamp;

@Entity
@Table(name = "group_meta")
@Getter
@Setter
@ToString
@RequiredArgsConstructor
public class GroupMetaEntity {
    @Id
    @Column(name = "group_name", updatable = false)
    String groupName;

    @Column(name = "owner", nullable = false)
    String owner;

    @Column(name = "created_time", nullable = false)
    Timestamp createdTime;

    public GroupMetaEntity(String groupName, String owner) {
        this.groupName = groupName;
        this.owner = owner;
        this.createdTime = new Timestamp(System.currentTimeMillis());
    }
}
