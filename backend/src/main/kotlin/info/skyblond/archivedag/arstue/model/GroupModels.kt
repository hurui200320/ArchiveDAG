package info.skyblond.archivedag.arstue.model

import info.skyblond.archivedag.arudaz.protos.group.GroupDetailResponse

data class GroupDetailModel(
    val groupName: String,
    val owner: String,
    val createTimestamp: Long
) {
    fun toProto(): GroupDetailResponse {
        return GroupDetailResponse.newBuilder()
            .setGroupName(this.groupName)
            .setOwner(this.owner)
            .setCreatedTimestamp(this.createTimestamp)
            .build()
    }
}
