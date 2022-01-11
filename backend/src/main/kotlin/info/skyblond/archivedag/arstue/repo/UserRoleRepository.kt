package info.skyblond.archivedag.arstue.repo

import info.skyblond.archivedag.arstue.entity.UserRoleEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.stereotype.Repository

@Repository
interface UserRoleRepository : JpaRepository<UserRoleEntity, String> {

    fun findAllByUsername(username: String): List<UserRoleEntity>

    fun findAllByUsername(username: String, pageable: Pageable): Page<UserRoleEntity>

    fun findByUsernameAndRole(username: String, role: String): UserRoleEntity?

    fun existsByUsernameAndRole(username: String, role: String): Boolean

    @Modifying
    fun deleteByUsernameAndRole(username: String, role: String)

    @Modifying
    fun deleteAllByUsername(username: String)
}
