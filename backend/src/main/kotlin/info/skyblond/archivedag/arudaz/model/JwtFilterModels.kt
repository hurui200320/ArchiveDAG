package info.skyblond.archivedag.arudaz.model

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonProperty

data class JwtAuthRequest(
    @JsonProperty("username")
    val username: String,
    @JsonProperty("password")
    val password: String
)


data class RefreshJWTRequest(
    @JsonProperty("old_token")
    @JsonAlias("oldToken")
    val oldToken: String
)

data class JWTAuthResponse(
    @JsonProperty("token")
    val token: String
)
