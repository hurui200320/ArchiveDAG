package info.skyblond.archivedag.model.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.*;
import java.sql.Timestamp;

@Entity
@Table(name = "cert_table")
@Getter
@Setter
@ToString
@RequiredArgsConstructor
public class CertEntity {
    @Id
    @Column(name = "serial_number", updatable = false)
    String serialNumber;

    @Column(name = "username", updatable = false, nullable = false)
    String username;

    @Column(name = "issued_time", nullable = false)
    Timestamp issuedTime;

    @Column(name = "expired_at", nullable = false)
    Timestamp expiredTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    Status status;

    public CertEntity(String serialNumber, String username, Timestamp issuedTime, Timestamp expiredTime) {
        this(serialNumber, username, issuedTime, expiredTime, Status.LOCKED);
    }

    public CertEntity(String serialNumber, String username, Timestamp issuedTime, Timestamp expiredTime, Status status) {
        this.serialNumber = serialNumber;
        this.username = username;
        this.issuedTime = issuedTime;
        this.expiredTime = expiredTime;
        this.status = status;
    }

    public enum Status {
        ENABLED, DISABLED, REVOKED, LOCKED
    }
}
