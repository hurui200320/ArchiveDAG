package info.skyblond.archivedag.arstue.entity

import java.sql.Timestamp
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "group_meta")
class GroupMetaEntity(
    @Id
    @Column(name = "group_name", updatable = false)
    val groupName: String,

    @Column(name = "owner", nullable = false)
    val owner: String,

    @Column(name = "created_time", nullable = false)
    val createdTime: Timestamp,
) {
    constructor(groupName: String, owner: String)
            : this(groupName, owner, Timestamp(System.currentTimeMillis()))
}
