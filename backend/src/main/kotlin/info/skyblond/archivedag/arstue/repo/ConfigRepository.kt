package info.skyblond.archivedag.arstue.repo

import info.skyblond.archivedag.arstue.entity.ConfigEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ConfigRepository : JpaRepository<ConfigEntity, String> {

    fun findAllByKeyStartingWith(keyPrefix: String, pageable: Pageable): Page<ConfigEntity>

    fun findByKey(key: String): ConfigEntity?
}
