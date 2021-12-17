package info.skyblond.archivedag.model.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "group_user")
@Getter
@Setter
@ToString
@RequiredArgsConstructor
@IdClass(GroupUserEntity.class)
public class GroupUserEntity implements Serializable {
    @Id
    @Column(name = "group_name", updatable = false)
    String groupName;

    @Id
    @Column(name = "username", nullable = false)
    String username;

    public GroupUserEntity(String groupName, String username) {
        this.groupName = groupName;
        this.username = username;
    }
}
