package info.skyblond.archivedag.model

import com.fasterxml.jackson.annotation.JsonProperty

data class JwtAuthRequest(
    @JsonProperty("username")
    val username: String,
    @JsonProperty("password")
    val password: String
)

data class JWTAuthResponse(
    @JsonProperty("token")
    val token: String
)
