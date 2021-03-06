package info.skyblond.archivedag.arstue.entity

import java.sql.Timestamp
import javax.persistence.*

@Entity
@Table(name = "cert_table")
class CertEntity(
    @Id
    @Column(name = "serial_number", updatable = false)
    val serialNumber: String,

    @Column(name = "username", updatable = false, nullable = false)
    val username: String,

    @Column(name = "issued_time", nullable = false)
    val issuedTime: Timestamp,

    @Column(name = "expired_at", nullable = false)
    val expiredTime: Timestamp,
) {
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: Status = Status.DISABLED

    enum class Status {
        ENABLED, DISABLED
    }
}
