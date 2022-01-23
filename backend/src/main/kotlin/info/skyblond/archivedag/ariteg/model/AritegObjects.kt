package info.skyblond.archivedag.ariteg.model

import com.google.protobuf.ByteString
import com.google.protobuf.GeneratedMessageV3
import info.skyblond.archivedag.ariteg.protos.*
import io.ipfs.multihash.Multihash
import javax.annotation.concurrent.Immutable

interface AritegObject {
    fun toProto(): GeneratedMessageV3
    fun getObjectType(): AritegObjectType
}

/**
 * Blob Object, store data only.
 * */
@Immutable
data class BlobObject(
    val data: ByteString
) : AritegObject {
    companion object {
        @JvmStatic
        fun fromProto(proto: AritegBlobObject): BlobObject {
            return BlobObject(proto.data)
        }
    }

    override fun toProto(): AritegBlobObject {
        return AritegBlobObject.newBuilder()
            .setData(data)
            .build()
    }

    override fun getObjectType(): AritegObjectType {
        return AritegObjectType.BLOB
    }
}

/**
 * List Object, concat list of blob/list to get data.
 * */
@Immutable
data class ListObject(
    val list: List<AritegLink>
) : AritegObject {
    companion object {
        @JvmStatic
        fun fromProto(proto: AritegListObject): ListObject {
            return ListObject(proto.linksList)
        }
    }

    override fun toProto(): AritegListObject {
        return AritegListObject.newBuilder()
            .addAllLinks(list)
            .build()
    }

    override fun getObjectType(): AritegObjectType {
        return AritegObjectType.LIST
    }
}

/**
 * Tree Object, represent a folder-like structure with links.
 * */
@Immutable
data class TreeObject(
    val links: List<AritegLink>
) : AritegObject {
    companion object {
        @JvmStatic
        fun fromProto(proto: AritegTreeObject): TreeObject {
            return TreeObject(proto.linksList)
        }
    }

    override fun toProto(): AritegTreeObject {
        return AritegTreeObject.newBuilder()
            .addAllLinks(links)
            .build()
    }

    override fun getObjectType(): AritegObjectType {
        return AritegObjectType.TREE
    }
}

/**
 * Commit Object, representing a snapshot of an object at some time.
 * */
@Immutable
data class CommitObject(
    val unixTimestamp: Long,
    val message: String,
    val parentLink: AritegLink,
    val committedObjectLink: AritegLink,
    val authorLink: AritegLink,
) : AritegObject {
    companion object {
        @JvmStatic
        fun fromProto(proto: AritegCommitObject): CommitObject {
            return CommitObject(
                unixTimestamp = proto.unixTimestamp,
                message = proto.message,
                parentLink = proto.parent,
                committedObjectLink = proto.committedObject,
                authorLink = proto.author,
            )
        }
    }

    override fun toProto(): AritegCommitObject {
        return AritegCommitObject.newBuilder()
            .setUnixTimestamp(unixTimestamp)
            .setMessage(message)
            .setParent(parentLink)
            .setCommittedObject(committedObjectLink)
            .setAuthor(authorLink)
            .build()
    }

    override fun getObjectType(): AritegObjectType {
        return AritegObjectType.COMMIT
    }
}

/**
 * A collection of useful methods
 * */
object AritegObjects {
    /**
     * Map to [BlobObject.fromProto]
     * */
    @JvmStatic
    fun fromProto(proto: AritegBlobObject): BlobObject = BlobObject.fromProto(proto)

    /**
     * Map to [ListObject.fromProto]
     * */
    @JvmStatic
    fun fromProto(proto: AritegListObject): ListObject = ListObject.fromProto(proto)

    /**
     * Map to [TreeObject.fromProto]
     * */
    @JvmStatic
    fun fromProto(proto: AritegTreeObject): TreeObject = TreeObject.fromProto(proto)

    /**
     * Map to [CommitObject.fromProto]
     * */
    @JvmStatic
    fun fromProto(proto: AritegCommitObject): CommitObject = CommitObject.fromProto(proto)

    /**
     * Generate a new link from the given multihash and object type.
     * Name is empty.
     * */
    @JvmStatic
    fun newLink(multihash: Multihash, type: AritegObjectType): AritegLink = newLink("", multihash, type)

    /**
     * Generate a new link from the given name, multihash and object type.
     * */
    @JvmStatic
    fun newLink(name: String, multihash: Multihash, type: AritegObjectType): AritegLink = AritegLink.newBuilder()
        .setName(name).setMultihash(ByteString.copyFrom(multihash.toBytes())).setType(type).build()
}
