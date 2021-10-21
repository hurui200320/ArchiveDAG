package info.skyblond.archivedag.model.ao

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.google.protobuf.ByteString
import info.skyblond.archivedag.model.bo.RestoreOption
import info.skyblond.ariteg.AritegLink
import info.skyblond.ariteg.ObjectType
import info.skyblond.ariteg.objects.CommitObject
import info.skyblond.ariteg.objects.toMultihash
import io.ipfs.multihash.Multihash

data class AritegLinkModel(
    @JsonProperty("name")
    val name: String,
    @JsonProperty("link")
    val link: String,
    @JsonProperty("type")
    val type: String
) {
    companion object {
        @JvmStatic
        fun fromAritegLink(aritegLink: AritegLink): AritegLinkModel = AritegLinkModel(
            name = aritegLink.name,
            link = aritegLink.multihash.toMultihash().toBase58(),
            type = aritegLink.type.name
        )
    }

    fun toAritegLink(): AritegLink = AritegLink.newBuilder()
        .setName(this.name)
        .setMultihash(ByteString.copyFrom(Multihash.fromBase58(this.link).toBytes()))
        .setType(ObjectType.valueOf(this.type)).build()
}

data class CreateTreeRequest(
    @JsonProperty("name")
    val name: String,
    @JsonProperty("links")
    val links: List<AritegLinkModel>
) {
    @JsonIgnore
    fun getAritegLinks(): List<AritegLink> = links.map { it.toAritegLink() }
}

data class SimplifiedLinkModel(
    @JsonProperty("link")
    val link: String,
    @JsonProperty("type")
    val type: String
) {
    companion object {
        @JvmStatic
        fun fromAritegLink(aritegLink: AritegLink): SimplifiedLinkModel = SimplifiedLinkModel(
            link = aritegLink.multihash.toMultihash().toBase58(),
            type = aritegLink.type.name
        )
    }

    fun toAritegLink(name: String): AritegLink = AritegLink.newBuilder()
        .setName(name)
        .setMultihash(ByteString.copyFrom(Multihash.fromBase58(this.link).toBytes()))
        .setType(ObjectType.valueOf(this.type)).build()
}

data class CreateCommitRequest(
    @JsonProperty("name")
    val name: String,
    @JsonProperty("unix_timestamp")
    val unixTimestamp: Long,
    @JsonProperty("commit_message")
    val message: String,
    @JsonProperty("parent_link")
    val parent: SimplifiedLinkModel?,
    @JsonProperty("commit_link")
    val committedObject: SimplifiedLinkModel,
    @JsonProperty("author_link")
    val author: SimplifiedLinkModel,
) {
    @JsonIgnore
    fun getParentAritegLink(): AritegLink {
        return parent?.toAritegLink("parent") ?: AritegLink.newBuilder()
            .setName("commit_data")
            .setMultihash(ByteString.EMPTY)
            .build()
    }

    @JsonIgnore
    fun getCommittedObjectAritegLink(): AritegLink {
        return committedObject.toAritegLink("commit_data")
    }

    @JsonIgnore
    fun getAuthorAritegLink(): AritegLink {
        return author.toAritegLink("author")
    }
}

data class CommitObjectModel(
    @JsonProperty("unix_timestamp")
    val unixTimestamp: Long,
    @JsonProperty("commit_message")
    val message: String,
    @JsonProperty("parent_link")
    val parentLink: SimplifiedLinkModel?,
    @JsonProperty("commit_link")
    val committedObjectLink: SimplifiedLinkModel,
    @JsonProperty("author_link")
    val authorLink: SimplifiedLinkModel,
) {
    companion object {
        @JvmStatic
        fun fromCommitObject(obj: CommitObject): CommitObjectModel = CommitObjectModel(
            obj.unixTimestamp, obj.message,
            if (obj.parentLink.multihash.isEmpty) null else SimplifiedLinkModel.fromAritegLink(obj.parentLink),
            SimplifiedLinkModel.fromAritegLink(obj.committedObjectLink),
            SimplifiedLinkModel.fromAritegLink(obj.authorLink)
        )
    }
}

data class RestoreRequest(
    @JsonProperty("links")
    val links: List<String>,
    @JsonProperty("days")
    val days: Int
) {
    fun toRestoreOption(): RestoreOption = RestoreOption(days)
}

data class StatusModel(
    @JsonProperty("type")
    val type: String,
    @JsonProperty("media_type")
    val mediaType: String,
    @JsonProperty("status")
    val status: String,
    @JsonProperty("raw_size")
    val rawSize: Int,
    // TODO more data?
)
