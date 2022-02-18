package info.skyblond.archivedag.arstue.repo

import info.skyblond.archivedag.arstue.entity.RecordAccessControlEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface RecordAccessControlRepository : JpaRepository<RecordAccessControlEntity, String> {
    fun existsByRecordIdAndTypeAndTarget(
        recordId: UUID, type: RecordAccessControlEntity.Type, target: String
    ): Boolean

    fun findByRecordIdAndTypeAndTarget(
        recordId: UUID, type: RecordAccessControlEntity.Type, target: String
    ): RecordAccessControlEntity?

    @Modifying
    fun deleteByRecordIdAndTypeAndTarget(
        recordId: UUID, type: RecordAccessControlEntity.Type, target: String
    )

    @Modifying
    fun deleteAllByTypeAndTarget(
        type: RecordAccessControlEntity.Type, target: String
    )

    @Modifying
    fun deleteAllByRecordId(recordId: UUID)

    fun findAllByRecordId(recordId: UUID, pageable: Pageable): Page<RecordAccessControlEntity>

    fun findAllByTypeAndTargetAndPermissionContains(
        type: RecordAccessControlEntity.Type,
        target: String,
        permission: String,
        pageable: Pageable
    ): Page<RecordAccessControlEntity>
}
