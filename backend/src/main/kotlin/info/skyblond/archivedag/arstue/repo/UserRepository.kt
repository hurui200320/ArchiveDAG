package info.skyblond.archivedag.arstue.repo

import info.skyblond.archivedag.arstue.entity.UserEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface UserRepository : JpaRepository<UserEntity, String> {
    fun findByUsername(username: String): UserEntity?

    fun findAllByUsernameContaining(keyword: String, pageable: Pageable): Page<UserEntity>

    fun existsByUsername(username: String): Boolean

    @Modifying
    @Query("update UserEntity u set u.password = ?2 where u.username = ?1")
    fun updateUserPassword(username: String, password: String)

    @Modifying
    @Query("update UserEntity u set u.status = ?2 where u.username = ?1")
    fun updateUserStatus(username: String, status: UserEntity.Status)

    @Modifying
    fun deleteByUsername(username: String)
}
