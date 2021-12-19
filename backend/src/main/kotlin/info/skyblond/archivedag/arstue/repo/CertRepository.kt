package info.skyblond.archivedag.arstue.repo

import info.skyblond.archivedag.arstue.entity.CertEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.sql.Timestamp

interface CertRepository : JpaRepository<CertEntity, String> {
    fun existsBySerialNumber(serialNumber: String): Boolean

    fun findAllByUsernameContainingAndIssuedTimeBetweenAndExpiredTimeBetween(
        ownerKeyword: String, issueStart: Timestamp, issueEnd: Timestamp,
        expireStart: Timestamp, expireEnd: Timestamp, pageable: Pageable
    ): Page<CertEntity>

    fun findAllByUsernameAndIssuedTimeBetweenAndExpiredTimeBetween(
        owner: String, issueStart: Timestamp, issueEnd: Timestamp,
        expireStart: Timestamp, expireEnd: Timestamp, pageable: Pageable
    ): Page<CertEntity>

    fun existsBySerialNumberAndUsername(serialNumber: String, username: String): Boolean

    fun findBySerialNumber(serialNumber: String): CertEntity?

    @Modifying
    @Query("update CertEntity c set c.status = ?2 where c.serialNumber = ?1")
    fun updateCertStatus(serialNumber: String, status: CertEntity.Status)

    @Modifying
    fun deleteBySerialNumber(serialNumber: String)

    @Modifying
    fun deleteAllByUsername(username: String)

}
