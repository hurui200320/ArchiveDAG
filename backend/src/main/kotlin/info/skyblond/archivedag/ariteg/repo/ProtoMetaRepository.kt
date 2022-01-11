package info.skyblond.archivedag.ariteg.repo

import info.skyblond.archivedag.ariteg.entity.ProtoMetaEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface ProtoMetaRepository : JpaRepository<ProtoMetaEntity, String> {
    fun findByPrimaryHash(primaryHash: String): ProtoMetaEntity?

    fun existsByPrimaryHash(primaryHash: String): Boolean

    fun existsByPrimaryHashAndSecondaryHash(primaryHash: String, secondaryHash: String): Boolean

    @Modifying
    @Query("update ProtoMetaEntity m set m.mediaType = ?2 where m.primaryHash = ?1")
    fun updateMediaType(primaryHash: String, mediaType: String?)

    @Modifying
    fun deleteByPrimaryHash(primaryHash: String)
}
