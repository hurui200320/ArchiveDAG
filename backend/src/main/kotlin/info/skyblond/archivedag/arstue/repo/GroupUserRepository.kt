package info.skyblond.archivedag.arstue.repo

import info.skyblond.archivedag.arstue.entity.GroupUserEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying

interface GroupUserRepository : JpaRepository<GroupUserEntity, String> {

    @Modifying
    fun deleteAllByGroupName(groupName: String)

    fun existsByGroupNameAndUsername(groupName: String, username: String): Boolean

    @Modifying
    fun deleteByGroupNameAndUsername(groupName: String, username: String)

    fun findAllByUsername(username: String, pageable: Pageable): Page<GroupUserEntity>
}
