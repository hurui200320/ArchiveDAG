package info.skyblond.archivedag.arstue.entity

import javax.persistence.*

@Entity
@Table(name = "user_table")
class UserEntity(
    @Id
    @Column(name = "username", updatable = false)
    val username: String,

    @Column(name = "password", nullable = false)
    val password: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    val status: Status
) {

    constructor(username: String, password: String) : this(username, password, Status.LOCKED)

    enum class Status {
        ENABLED, DISABLED, LOCKED
    }
}
