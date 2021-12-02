package info.skyblond.archivedag.model.entity;

import lombok.*;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "user_role")
@Getter
@Setter
@ToString
@RequiredArgsConstructor
@IdClass(UserRoleEntity.class)
public class UserRoleEntity implements Serializable {
    @Id
    @Column(name = "username", updatable = false)
    String username;

    @Id
    @Column(name = "role_name", nullable = false)
    String role;

    public UserRoleEntity(String username, String role) {
        this.username = username;
        this.role = role;
    }
}
