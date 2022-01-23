package info.skyblond.archivedag.arstue.model

import info.skyblond.archivedag.arstue.entity.RecordAccessControlEntity
import io.ipfs.multihash.Multihash
import java.util.*

data class FileRecordDetailModel(
    val recordId: UUID,
    val recordName: String,
    val multihash: Multihash?,
    val createdTimestamp: Long,
    val owner: String,
)

data class RecordACLElementModel(
    val type: RecordAccessControlEntity.Type,
    val target: String,
    val permission: String
)
