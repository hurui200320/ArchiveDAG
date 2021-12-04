package info.skyblond.archivedag.repo;

import info.skyblond.archivedag.model.entity.CertEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.sql.Timestamp;
import java.util.List;

public interface CertRepository extends JpaRepository<CertEntity, String> {
    boolean existsBySerialNumber(String serialNumber);

    Page<CertEntity> findAllByUsernameContainingAndIssuedTimeBetweenAndExpiredTimeBetween(
            String ownerKeyword, Timestamp issueStart, Timestamp issueEnd,
            Timestamp expireStart, Timestamp expireEnd, Pageable pageable);

    Page<CertEntity> findAllByUsernameAndIssuedTimeBetweenAndExpiredTimeBetween(
            String owner, Timestamp issueStart, Timestamp issueEnd,
            Timestamp expireStart, Timestamp expireEnd, Pageable pageable);

    boolean existsBySerialNumberAndUsername(String serialNumber, String username);

    CertEntity findBySerialNumber(String serialNumber);

    @Modifying
    @Query("update CertEntity c set c.status = ?2 where c.serialNumber = ?1")
    void updateCertStatus(String serialNumber, CertEntity.Status status);

    @Modifying
    void deleteBySerialNumber(String serialNumber);

    List<CertEntity> findAllByUsername(String username);

    Page<CertEntity> findAllByUsername(String username, Pageable pageable);

}
