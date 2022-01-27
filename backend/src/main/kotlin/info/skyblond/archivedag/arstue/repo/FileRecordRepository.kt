package info.skyblond.archivedag.arstue.repo

import info.skyblond.archivedag.arstue.entity.FileRecordEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface FileRecordRepository : JpaRepository<FileRecordEntity, UUID> {
    fun existsByRecordId(recordId: UUID): Boolean

    fun existsByRecordIdAndOwner(recordId: UUID, owner: String): Boolean

    @Modifying
    @Query("update FileRecordEntity r set r.multihash = ?2 where r.recordId = ?1")
    fun updateRecordRef(recordId: UUID, newRef: String?)

    @Modifying
    @Query("update FileRecordEntity r set r.recordName = ?2 where r.recordId = ?1")
    fun updateRecordName(recordId: UUID, newName: String)

    @Modifying
    @Query("update FileRecordEntity r set r.owner = ?2 where r.recordId = ?1")
    fun updateRecordOwner(recordId: UUID, newOwner: String)

    @Modifying
    fun deleteByRecordId(recordId: UUID)

    fun findByRecordId(recordId: UUID): FileRecordEntity?

    fun findAllByOwner(owner: String, pageable: Pageable): Page<FileRecordEntity>

}
