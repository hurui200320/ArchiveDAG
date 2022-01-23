package info.skyblond.archivedag.arstue.entity

import java.sql.Timestamp
import java.util.*
import javax.persistence.*

@Entity
@Table(name = "file_record")
class FileRecordEntity(
    @Column(name = "record_name", nullable = false)
    val name: String,

    @Column(name = "multihash", nullable = true)
    val multihash: String?,

    @Column(name = "created_time", nullable = false)
    val createdTime: Timestamp,

    @Column(name = "owner", nullable = false)
    val owner: String,
) {
    /**
     * This field must be null so that JPA can generate a UUID and set the value.
     * Any pre-set value will prevent JPA give the new value to the field.
     * */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "record_id", updatable = false)
    val recordId: UUID? = null

    constructor(
        recordName: String, owner: String
    ) : this(
        name = recordName,
        multihash = null,
        createdTime = Timestamp(System.currentTimeMillis()),
        owner = owner
    )
}
