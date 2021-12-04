package info.skyblond.archivedag.model.bo

import info.skyblond.archivedag.model.entity.UserEntity

data class UserDetailModel(
    val username: String,
    val status: UserEntity.Status,
    val roles: List<String>
)
