package info.skyblond.archivedag.arstue

import info.skyblond.archivedag.arstue.entity.UserEntity
import info.skyblond.archivedag.arstue.entity.UserRoleEntity
import info.skyblond.archivedag.arstue.model.UserDetailModel
import info.skyblond.archivedag.arstue.repo.CertRepository
import info.skyblond.archivedag.arstue.repo.UserRepository
import info.skyblond.archivedag.arstue.repo.UserRoleRepository
import info.skyblond.archivedag.arstue.service.PatternService
import info.skyblond.archivedag.commons.DuplicatedEntityException
import info.skyblond.archivedag.commons.EntityNotFoundException
import org.springframework.data.domain.Pageable
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
class UserManagementService(
    private val userRepository: UserRepository,
    private val userRoleRepository: UserRoleRepository,
    private val certRepository: CertRepository,
    private val passwordEncoder: PasswordEncoder,
    private val patternService: PatternService,
    private val fileRecordService: FileRecordService
) {
    fun listUsername(keyword: String, pageable: Pageable): List<String> {
        val result: MutableList<String> = LinkedList()
        userRepository.findAllByUsernameContaining(keyword, pageable)
            .forEach { result.add(it.username) }
        return result
    }

    fun userExists(username: String): Boolean {
        return userRepository.existsByUsername(username)
    }

    fun queryUser(username: String): UserDetailModel {
        val entity = userRepository.findByUsername(username)
            ?: throw EntityNotFoundException("User $username")
        val roles: MutableList<String> = LinkedList()
        userRoleRepository.findAllByUsername(username)
            .forEach { roles.add(it.role) }
        return UserDetailModel(
            entity.username,
            entity.status,
            roles
        )
    }

    fun listUserRoles(username: String, pageable: Pageable): List<String> {
        val roles: MutableList<String> = LinkedList()
        userRoleRepository.findAllByUsername(username, pageable)
            .forEach { roles.add(it.role) }
        return roles
    }

    @Transactional
    fun changePassword(username: String, password: String) {
        if (!userRepository.existsByUsername(username)) {
            throw EntityNotFoundException("User $username")
        }
        val encodedPassword = passwordEncoder.encode(password)
        userRepository.updateUserPassword(username, encodedPassword)
    }

    @Transactional
    fun changeStatus(username: String, status: UserEntity.Status) {
        if (!userRepository.existsByUsername(username)) {
            throw EntityNotFoundException("User $username")
        }
        userRepository.updateUserStatus(username, status)
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    fun createUser(username: String, password: String) {
        require(patternService.isValidUsername(username)) { "In valid username. The username must meet the regex: " + patternService.usernameRegex }
        val entity = UserEntity(username, passwordEncoder.encode(password))
        if (userRepository.existsByUsername(username)) {
            throw DuplicatedEntityException("user")
        }
        userRepository.save(entity)
    }

    @Transactional
    fun deleteUser(username: String) {
        if (!userRepository.existsByUsername(username)) {
            throw EntityNotFoundException("User $username")
        }
        // if there are file records, refuse to delete
        if (fileRecordService.listOwnedRecords(username, Pageable.ofSize(10)).isNotEmpty()) {
            throw IllegalStateException("User $username still have file records")
        }
        // delete certs
        certRepository.deleteAllByUsername(username)
        // delete roles
        userRoleRepository.deleteAllByUsername(username)
        // delete user
        userRepository.deleteByUsername(username)
    }

    @Transactional
    fun addRoleToUser(username: String, role: String) {
        if (!userRepository.existsByUsername(username)) {
            throw EntityNotFoundException("User $username")
        }
        val entity = UserRoleEntity(username, role)
        if (userRoleRepository.existsByUsernameAndRole(username, role)) {
            throw DuplicatedEntityException("role for user")
        }
        userRoleRepository.save(entity)
    }

    @Transactional
    fun removeRoleFromUser(username: String, role: String) {
        if (!userRepository.existsByUsername(username)) {
            throw EntityNotFoundException("User $username")
        }
        if (!userRoleRepository.existsByUsernameAndRole(username, role)) {
            throw EntityNotFoundException("Role $role for user $username")
        }
        userRoleRepository.deleteByUsernameAndRole(username, role)
    }
}
