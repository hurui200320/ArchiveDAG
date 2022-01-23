package info.skyblond.archivedag.arstue

import info.skyblond.archivedag.arstue.entity.GroupMetaEntity
import info.skyblond.archivedag.arstue.entity.GroupUserEntity
import info.skyblond.archivedag.arstue.model.GroupDetailModel
import info.skyblond.archivedag.arstue.repo.GroupMetaRepository
import info.skyblond.archivedag.arstue.repo.GroupUserRepository
import info.skyblond.archivedag.arstue.service.PatternService
import info.skyblond.archivedag.commons.DuplicatedEntityException
import info.skyblond.archivedag.commons.EntityNotFoundException
import info.skyblond.archivedag.commons.getUnixTimestamp
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import java.util.*
import javax.transaction.Transactional

@Service
class GroupService(
    private val patternService: PatternService,
    private val groupMetaRepository: GroupMetaRepository,
    private val groupUserRepository: GroupUserRepository
) {
    @Transactional
    fun createGroup(groupName: String, owner: String) {
        require(patternService.isValidGroupName(groupName)) { "In valid group name. The group name must meet the regex: " + patternService.groupNameRegex }
        val entity = GroupMetaEntity(groupName, owner)
        if (groupMetaRepository.existsByGroupName(groupName)) {
            throw DuplicatedEntityException(groupName)
        }
        groupMetaRepository.save(entity)
    }

    @Transactional
    fun deleteGroup(groupName: String) {
        if (!groupMetaRepository.existsByGroupName(groupName)) {
            throw EntityNotFoundException(groupName)
        }
        // delete all member
        groupUserRepository.deleteAllByGroupName(groupName)
        // delete meta data
        groupMetaRepository.deleteByGroupName(groupName)
    }

    fun queryGroupMeta(groupName: String): GroupDetailModel {
        val entity = groupMetaRepository.findByGroupName(groupName)
            ?: throw EntityNotFoundException(groupName)
        return GroupDetailModel(
            entity.groupName,
            entity.owner,
            getUnixTimestamp(entity.createdTime.time)
        )
    }

    @Transactional
    fun setGroupOwner(groupName: String, owner: String) {
        if (!groupMetaRepository.existsByGroupName(groupName)) {
            throw EntityNotFoundException(groupName)
        }
        groupMetaRepository.updateGroupOwner(groupName, owner)
    }

    @Transactional
    fun addUserToGroup(groupName: String, username: String) {
        if (!groupMetaRepository.existsByGroupName(groupName)) {
            throw EntityNotFoundException(groupName)
        }
        val entity = GroupUserEntity(groupName, username)
        if (groupUserRepository.existsByGroupNameAndUsername(groupName, username)) {
            throw DuplicatedEntityException("group member")
        }
        groupUserRepository.save(entity)
    }

    @Transactional
    fun removeUserFromGroup(groupName: String, username: String) {
        if (!groupMetaRepository.existsByGroupName(groupName)) {
            throw EntityNotFoundException(groupName)
        }
        if (!groupUserRepository.existsByGroupNameAndUsername(groupName, username)) {
            throw EntityNotFoundException("User $username in $groupName")
        }
        groupUserRepository.deleteByGroupNameAndUsername(groupName, username)
    }

    fun listUserOwnedGroup(username: String, pageable: Pageable): List<String> {
        val result: MutableList<String> = LinkedList()
        groupMetaRepository.findAllByOwner(username, pageable)
            .forEach { result.add(it.groupName) }
        return result
    }

    fun listUserJoinedGroup(username: String, pageable: Pageable): List<String> {
        val result: MutableList<String> = LinkedList()
        groupUserRepository.findAllByUsername(username, pageable)
            .forEach { result.add(it.groupName) }
        return result
    }

    fun userIsGroupMember(groupName: String, username: String): Boolean {
        return groupUserRepository.existsByGroupNameAndUsername(groupName, username)
    }

    fun userIsGroupOwner(groupName: String, username: String): Boolean {
        return groupMetaRepository.existsByGroupNameAndOwner(groupName, username)
    }

    fun listGroupName(keyword: String, pageable: Pageable): List<String> {
        val result: MutableList<String> = LinkedList()
        groupMetaRepository.findAllByGroupNameContains(keyword, pageable)
            .forEach { result.add(it.groupName) }
        return result
    }
}
