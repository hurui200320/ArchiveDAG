package info.skyblond.archivedag.arudaz.model

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonProperty
import info.skyblond.archivedag.arstue.entity.UserEntity

data class UserChangePasswordRequest(
    @JsonProperty("username")
    val username: String,
    @JsonProperty("new_password")
    @JsonAlias("newPassword")
    val newPassword: String
)

data class UserChangeStatusRequest(
    @JsonProperty("username")
    val username: String,
    @JsonProperty("new_status")
    @JsonAlias("newStatus")
    val newStatus: UserEntity.Status
)

data class CreateUserRequest(
    @JsonProperty("username")
    val username: String,
    @JsonProperty("password")
    val password: String
)

data class UserRoleRequest(
    @JsonProperty("username")
    val username: String,
    @JsonProperty("role")
    val role: String
)
