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

    @Column(name = "media_type")
    val mediaType: String?,

    @Column(name = "mark", nullable = false)
    val mark: String
) {
    constructor(
        primaryHash: String,
        secondaryHash: String,
        objectType: AritegObjectType,
        mediaType: String?
    ) : this(primaryHash, secondaryHash, objectType, mediaType, "")
}
