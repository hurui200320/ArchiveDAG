package info.skyblond.archivedag.arudaz.controller.http

import com.google.gson.Gson
import info.skyblond.archivedag.ariteg.config.EmbeddedRedisConfiguration
import info.skyblond.archivedag.arstue.entity.UserEntity
import info.skyblond.archivedag.arstue.entity.UserRoleEntity
import info.skyblond.archivedag.arstue.repo.UserRepository
import info.skyblond.archivedag.arstue.repo.UserRoleRepository
import info.skyblond.archivedag.arudaz.model.controller.JWTAuthResponse
import info.skyblond.archivedag.arudaz.model.controller.JwtAuthRequest
import info.skyblond.archivedag.arudaz.security.JwtTokenManager
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers

@SpringBootTest(classes = [EmbeddedRedisConfiguration::class])
@ActiveProfiles("test")
@AutoConfigureMockMvc
internal class AuthControllerTest {
    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var passwordEncoder: PasswordEncoder

    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var userRoleRepository: UserRoleRepository

    @Autowired
    lateinit var gson: Gson

    @Autowired
    lateinit var tokenManager: JwtTokenManager

    @BeforeEach
    fun setUp() {
        userRepository.save(
            UserEntity(
                "test_user_jwt",
                passwordEncoder.encode("password"),
                UserEntity.Status.ENABLED
            )
        )
        userRoleRepository.save(UserRoleEntity("test_user_jwt", "ROLE_USER"))
    }

    @Test
    fun testSignJwt() {
        val resp = mockMvc
            .perform(
                MockMvcRequestBuilders.post("/public/auth")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(gson.toJson(JwtAuthRequest("test_user_jwt", "password")))
            )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn().response.contentAsString
        val (token) = gson.fromJson(resp, JWTAuthResponse::class.java)
        Assertions.assertEquals(
            "test_user_jwt",
            tokenManager.getUsernameFromToken(token)
        )
    }

    @Test
    fun testWrongPassword() {
        mockMvc
            .perform(
                MockMvcRequestBuilders.post("/public/auth")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(gson.toJson(JwtAuthRequest("test_user_jwt", "not_password")))
            )
            .andExpect(MockMvcResultMatchers.status().isUnauthorized)
    }

    @Test
    fun testSpringSecurity() {
        mockMvc.perform(MockMvcRequestBuilders.get("/something_404"))
            .andExpect(MockMvcResultMatchers.status().isUnauthorized)
    }
}
