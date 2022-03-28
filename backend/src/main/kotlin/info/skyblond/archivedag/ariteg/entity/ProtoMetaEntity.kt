package info.skyblond.archivedag.ariteg.entity

import info.skyblond.archivedag.ariteg.protos.AritegObjectType
import javax.persistence.*

@Entity
@Table(name = "proto_meta")
class ProtoMetaEntity(
    @Id
    @Column(name = "primary_hash", updatable = false)
    val primaryHash: String,

    @Column(name = "secondary_hash", updatable = false, nullable = false)
    val secondaryHash: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "object_type", updatable = false, nullable = false)
    val objectType: AritegObjectType,
)
