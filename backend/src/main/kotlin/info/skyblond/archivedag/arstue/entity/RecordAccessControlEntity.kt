package info.skyblond.archivedag.arstue.entity

import java.io.Serializable
import java.util.*
import javax.persistence.*

@Entity
@Table(name = "file_record_access")
@IdClass(RecordAccessControlEntity.IdClass::class)
class RecordAccessControlEntity(
    @Id
    @Column(name = "record_id", updatable = false)
    val recordId: UUID,

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, updatable = false)
    val type: Type,

    @Id
    @Column(name = "target", nullable = false, updatable = false)
    val target: String,

    @Column(name = "permission", nullable = false)
    val permission: String
) {
    internal class IdClass : Serializable {
        lateinit var recordId: UUID
        lateinit var type: Type
        lateinit var target: String
    }

    enum class Type {
        USER, GROUP, OTHER
    }
}
