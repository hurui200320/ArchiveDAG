package info.skyblond.archivedag.model

import info.skyblond.archivedag.model.entity.UserEntity

data class UserDetailModel(
    val username: String,
    val status: UserEntity.UserStatus,
    val roles: List<String>
)
