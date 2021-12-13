package info.skyblond.archivedag.model.entity;

import info.skyblond.ariteg.protos.AritegObjectType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.*;

@Entity
@Table(name = "proto_meta")
@Getter
@Setter
@ToString
@RequiredArgsConstructor
public class ProtoMetaEntity {
    @Id
    @Column(name = "primary_hash", updatable = false)
    String primaryHash;

    @Column(name = "secondary_hash", updatable = false, nullable = false)
    String secondaryHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "object_type", updatable = false, nullable = false)
    AritegObjectType objectType;

    @Column(name = "media_type")
    String mediaType;

    @Column(name = "mark", nullable = false)
    String mark;

    public ProtoMetaEntity(String primaryHash, String secondaryHash, AritegObjectType objectType, String mediaType) {
        this(primaryHash, secondaryHash, objectType, mediaType, "");
    }

    public ProtoMetaEntity(String primaryHash, String secondaryHash, AritegObjectType objectType, String mediaType, String mark) {
        this.primaryHash = primaryHash;
        this.secondaryHash = secondaryHash;
        this.objectType = objectType;
        this.mediaType = mediaType;
        this.mark = mark;
    }
}
