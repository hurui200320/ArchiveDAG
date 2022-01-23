package info.skyblond.archivedag.arstue.entity

import java.io.Serializable
import javax.persistence.*

@Entity
@Table(name = "group_user")
@IdClass(GroupUserEntity::class)
class GroupUserEntity(
    @Id
    @Column(name = "group_name", updatable = false)
    val groupName: String,

    @Id
    @Column(name = "username", nullable = false)
    val username: String,
) : Serializable
