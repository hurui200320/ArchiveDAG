package info.skyblond.archivedag.model.entity;

import lombok.*;

import javax.persistence.*;

@Entity
@Table(name = "user_table")
@Getter
@Setter
@ToString
@RequiredArgsConstructor
public class UserEntity {
    @Id
    @Column(name = "username", updatable = false)
    String username;

    @Column(name = "password", nullable = false)
    String password;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    UserStatus status;

    public UserEntity(String username, String password) {
        this.username = username;
        this.password = password;
        this.status = UserStatus.LOCKED;
    }

    public UserEntity(String username, String password, UserStatus status) {
        this.username = username;
        this.password = password;
        this.status = status;
    }

    public enum UserStatus {
        ENABLED, DISABLED, LOCKED
    }
}
