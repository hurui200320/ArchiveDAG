package info.skyblond.archivedag.model.bo

import info.skyblond.archivedag.protos.group.GroupDetail

data class GroupDetailModel(
    val groupName: String,
    val owner: String,
    val createTimestamp: Long
) {
    fun toProto(): GroupDetail {
        return GroupDetail.newBuilder()
            .setGroupName(this.groupName)
            .setOwner(this.owner)
            .setCreatedTimestamp(this.createTimestamp)
            .build()
    }
}
