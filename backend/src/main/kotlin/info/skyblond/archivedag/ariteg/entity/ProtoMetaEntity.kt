package info.skyblond.archivedag.ariteg.entity

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "proto_meta")
class ProtoMetaEntity(
    @Id
    @Column(name = "primary_hash", updatable = false)
    val primaryHash: String,

    @Column(name = "secondary_hash", updatable = false, nullable = false)
    val secondaryHash: String,
)
