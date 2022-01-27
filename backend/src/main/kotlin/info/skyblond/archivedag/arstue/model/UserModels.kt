package info.skyblond.archivedag.arstue.model

import info.skyblond.archivedag.arstue.entity.UserEntity

data class UserDetailModel(
    val username: String,
    val status: UserEntity.Status,
    val roles: List<String>
)
