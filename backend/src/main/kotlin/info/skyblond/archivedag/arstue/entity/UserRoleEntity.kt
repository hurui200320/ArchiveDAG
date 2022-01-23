package info.skyblond.archivedag.arstue.entity

import java.io.Serializable
import javax.persistence.*

@Entity
@Table(name = "user_role")
@IdClass(UserRoleEntity::class)
class UserRoleEntity(
    @Id
    @Column(name = "username", updatable = false)
    val username: String,

    @Id
    @Column(name = "role_name", nullable = false)
    val role: String
) : Serializable
