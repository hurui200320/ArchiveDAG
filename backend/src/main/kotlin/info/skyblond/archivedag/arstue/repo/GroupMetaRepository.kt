package info.skyblond.archivedag.arstue.repo

import info.skyblond.archivedag.arstue.entity.GroupMetaEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface GroupMetaRepository : JpaRepository<GroupMetaEntity, String> {
    fun existsByGroupName(groupName: String): Boolean

    fun existsByGroupNameAndOwner(groupName: String, owner: String): Boolean

    @Modifying
    fun deleteByGroupName(groupName: String)

    fun findByGroupName(groupName: String): GroupMetaEntity?

    @Modifying
    @Query("update GroupMetaEntity m set m.owner = ?2 where m.groupName = ?1")
    fun updateGroupOwner(groupName: String, owner: String)

    fun findAllByOwner(owner: String, pageable: Pageable): Page<GroupMetaEntity>


    fun findAllByGroupNameContains(keyword: String, pageable: Pageable): Page<GroupMetaEntity>
}
