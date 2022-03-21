package info.skyblond.archivedag.arstue

import info.skyblond.archivedag.arstue.entity.FileRecordEntity
import info.skyblond.archivedag.arstue.entity.RecordAccessControlEntity
import info.skyblond.archivedag.arstue.model.FileRecordDetailModel
import info.skyblond.archivedag.arstue.model.RecordACLElementModel
import info.skyblond.archivedag.arstue.repo.FileRecordRepository
import info.skyblond.archivedag.arstue.repo.RecordAccessControlRepository
import info.skyblond.archivedag.commons.EntityNotFoundException
import info.skyblond.archivedag.commons.getUnixTimestamp
import io.ipfs.multihash.Multihash
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import java.util.*
import javax.transaction.Transactional

@Service
class FileRecordService(
    private val fileRecordRepository: FileRecordRepository,
    private val recordAccessControlRepository: RecordAccessControlRepository
) {
    companion object {
        const val READ_PERMISSION_BIT: Int = 1 shl 0
        const val READ_PERMISSION_CHAR: Char = 'r'

        const val UPDATE_REF_PERMISSION_BIT: Int = 1 shl 1
        const val UPDATE_REF_PERMISSION_CHAR: Char = 'u'

        const val UPDATE_NAME_PERMISSION_BIT: Int = 1 shl 2
        const val UPDATE_NAME_PERMISSION_CHAR: Char = 'n'

        const val FULL_PERMISSION: Int = READ_PERMISSION_BIT or
                UPDATE_REF_PERMISSION_BIT or
                UPDATE_NAME_PERMISSION_BIT

        @JvmStatic
        fun permissionStringToInt(permissionStr: String): Int {
            var result = 0
            for (c in permissionStr) {
                when (c) {
                    READ_PERMISSION_CHAR -> result = result or READ_PERMISSION_BIT
                    UPDATE_REF_PERMISSION_CHAR -> result = result or UPDATE_REF_PERMISSION_BIT
                    UPDATE_NAME_PERMISSION_CHAR -> result = result or UPDATE_NAME_PERMISSION_BIT
                }
            }
            return result
        }

        @JvmStatic
        fun permissionIntToString(permissionInt: Int): String {
            val stringBuilder = StringBuilder()

            if (permissionInt and READ_PERMISSION_BIT != 0) {
                stringBuilder.append(READ_PERMISSION_CHAR)
            }
            if (permissionInt and UPDATE_REF_PERMISSION_BIT != 0) {
                stringBuilder.append(UPDATE_REF_PERMISSION_CHAR)
            }
            if (permissionInt and UPDATE_NAME_PERMISSION_BIT != 0) {
                stringBuilder.append(UPDATE_NAME_PERMISSION_CHAR)
            }

            return stringBuilder.toString()
        }
    }

    @Transactional
    fun createRecord(recordName: String, owner: String): UUID {
        require(recordName.isNotBlank()) { "Record name cannot be blank" }
        val entity = FileRecordEntity(recordName, owner)
        fileRecordRepository.save(entity)
        return entity.recordId!!
    }

    @Transactional
    fun setRecordRef(recordId: UUID, multihash: Multihash) {
        if (!fileRecordRepository.existsByRecordId(recordId)) {
            throw EntityNotFoundException("Record #$recordId")
        }
        fileRecordRepository.updateRecordRef(recordId, multihash.toBase58())
    }

    @Transactional
    fun setRecordName(recordId: UUID, newName: String) {
        if (!fileRecordRepository.existsByRecordId(recordId)) {
            throw EntityNotFoundException("Record #$recordId")
        }
        require(newName.isNotBlank()) { "Record name cannot be blank" }
        fileRecordRepository.updateRecordName(recordId, newName)
    }

    @Transactional
    fun setRecordOwner(recordId: UUID, newOwner: String) {
        if (!fileRecordRepository.existsByRecordId(recordId)) {
            throw EntityNotFoundException("Record #$recordId")
        }
        fileRecordRepository.updateRecordOwner(recordId, newOwner)
    }

    @Transactional
    fun deleteRecord(recordId: UUID) {
        if (!fileRecordRepository.existsByRecordId(recordId)) {
            throw EntityNotFoundException("Record #$recordId")
        }
        fileRecordRepository.deleteByRecordId(recordId)
        recordAccessControlRepository.deleteAllByRecordId(recordId)
    }

    fun queryRecord(recordId: UUID): FileRecordDetailModel {
        val entity = fileRecordRepository.findByRecordId(recordId)
            ?: throw EntityNotFoundException("Record #$recordId")
        return FileRecordDetailModel(
            recordId = entity.recordId!!,
            recordName = entity.recordName,
            multihash = entity.multihash?.let { Multihash.fromBase58(it) },
            createdTimestamp = getUnixTimestamp(entity.createdTime.time),
            owner = entity.owner
        )
    }

    // get all owned records
    fun listOwnedRecords(username: String, pageable: Pageable): List<UUID> {
        val result: MutableList<UUID> = LinkedList()
        fileRecordRepository.findAllByOwnerOrderByRecordId(username, pageable)
            .forEach { result.add(it.recordId!!) }
        return result
    }

    @Transactional
    fun setAccessRule(recordId: UUID, type: RecordAccessControlEntity.Type, target: String, permission: Int) {
        if (!fileRecordRepository.existsByRecordId(recordId)) {
            throw EntityNotFoundException("Record #$recordId")
        }
        // if type is DEFAULT, then target should be empty
        val actualTarget = if (type == RecordAccessControlEntity.Type.OTHER) "" else target
        val entity = RecordAccessControlEntity(
            recordId, type, actualTarget, permissionIntToString(permission)
        )
        recordAccessControlRepository.save(entity)
    }

    @Transactional
    fun deleteAccessRule(recordId: UUID, type: RecordAccessControlEntity.Type, target: String) {
        if (!recordAccessControlRepository
                .existsByRecordIdAndTypeAndTarget(recordId, type, target)
        ) {
            throw EntityNotFoundException("Access rule #$recordId:$type:$target")
        }
        recordAccessControlRepository
            .deleteByRecordIdAndTypeAndTarget(recordId, type, target)
    }

    // return Type, target, permissionStr
    fun listAccessRules(
        recordId: UUID,
        pageable: Pageable
    ): List<RecordACLElementModel> {
        if (!fileRecordRepository.existsByRecordId(recordId)) {
            throw EntityNotFoundException("Record #$recordId")
        }
        val result = LinkedList<RecordACLElementModel>()
        recordAccessControlRepository.findAllByRecordId(recordId, pageable)
            .forEach { result.add(RecordACLElementModel(it.type, it.target, it.permission)) }
        return result
    }

    // list all records shared by user
    fun listUserSharedRecords(username: String, pageable: Pageable): List<UUID> {
        val result = LinkedList<UUID>()
        recordAccessControlRepository.findAllByTypeAndTargetAndPermissionContains(
            RecordAccessControlEntity.Type.USER, username, "r", pageable
        ).forEach { result.add(it.recordId) }
        return result
    }

    // list all records shared by group
    fun listGroupSharedRecords(groupName: String, pageable: Pageable): List<UUID> {
        val result = LinkedList<UUID>()
        recordAccessControlRepository.findAllByTypeAndTargetAndPermissionContains(
            RecordAccessControlEntity.Type.GROUP, groupName, "r", pageable
        ).forEach { result.add(it.recordId) }
        return result
    }

    // list all records where is public (default has read permission)
    fun listPublicSharedRecords(pageable: Pageable): List<UUID> {
        val result = LinkedList<UUID>()
        recordAccessControlRepository.findAllByTypeAndTargetAndPermissionContains(
            RecordAccessControlEntity.Type.OTHER, "", "r", pageable
        ).forEach { result.add(it.recordId) }
        return result
    }

    // get the permission for the given user (including his groups)
    // return the permission string for the given record
    fun queryPermission(recordId: UUID, username: String, groupNames: List<String>): Int {
        if (fileRecordRepository.existsByRecordIdAndOwner(recordId, username)) {
            // if owner, grant full access
            return FULL_PERMISSION
        }
        // get default value with user scope, 0 if no rules for user
        var permission = recordAccessControlRepository.findByRecordIdAndTypeAndTarget(
            recordId, RecordAccessControlEntity.Type.USER, username
        )?.let { permissionStringToInt(it.permission) } ?: 0
        // add group permission
        groupNames.forEach { group ->
            recordAccessControlRepository.findByRecordIdAndTypeAndTarget(
                recordId, RecordAccessControlEntity.Type.GROUP, group
            )?.let {
                permission = permission or permissionStringToInt(it.permission)
            }
        }
        // add other/public permission
        recordAccessControlRepository.findByRecordIdAndTypeAndTarget(
            recordId, RecordAccessControlEntity.Type.OTHER, ""
        )?.let {
            permission = permission or permissionStringToInt(it.permission)
        }
        // return the result
        return permission
    }
}
