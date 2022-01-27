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
) {
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: Status = Status.LOCKED

    constructor(username: String, password: String, status: Status) : this(username, password) {
        this.status = status
    }

    enum class Status {
        ENABLED, DISABLED, LOCKED
    }
}
