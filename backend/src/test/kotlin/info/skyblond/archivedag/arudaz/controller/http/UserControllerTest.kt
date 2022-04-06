package info.skyblond.archivedag.arudaz.controller.http

import com.google.gson.Gson
import info.skyblond.archivedag.arstue.entity.UserEntity
import info.skyblond.archivedag.arstue.entity.UserRoleEntity
import info.skyblond.archivedag.arstue.model.UserDetailModel
import info.skyblond.archivedag.arstue.repo.UserRepository
import info.skyblond.archivedag.arstue.repo.UserRoleRepository
import info.skyblond.archivedag.arudaz.model.CreateUserRequest
import info.skyblond.archivedag.arudaz.model.UserChangePasswordRequest
import info.skyblond.archivedag.arudaz.model.UserChangeStatusRequest
import info.skyblond.archivedag.arudaz.model.UserRoleRequest
import org.hamcrest.Matchers
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers

@SpringBootTest()
@ActiveProfiles("test")
@AutoConfigureMockMvc
internal class UserControllerTest {
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

    @BeforeEach
    fun setUp() {
        userRepository.save(
            UserEntity(
                "test_user_controller_user",
                passwordEncoder.encode("password"),
                UserEntity.Status.ENABLED
            )
        )
        userRoleRepository.save(UserRoleEntity("test_user_controller_user", "ROLE_USER"))
    }

    @WithMockUser(username = "test_user_admin", roles = ["ADMIN"])
    @Test
    fun testRemoveUserRoleUserNotFound() {
        userRepository.deleteByUsername("test_user_404")
        mockMvc
            .perform(
                MockMvcRequestBuilders.delete("/user/removeUserRole")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(gson.toJson(UserRoleRequest("test_user_404", "ROLE_USER")))
            )
            .andExpect(MockMvcResultMatchers.status().isNotFound)
    }

    @WithMockUser(username = "test_user_admin", roles = ["ADMIN"])
    @Test
    fun testAddAndRemoveUserRole() {
        userRepository.save(
            UserEntity(
                "test_user_role",
                passwordEncoder.encode("123456")
            )
        )
        userRoleRepository.deleteAllByUsername("test_user_role")
        // Add role
        mockMvc
            .perform(
                MockMvcRequestBuilders.post("/user/addUserRole")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(gson.toJson(UserRoleRequest("test_user_role", "ROLE_USER")))
            )
            .andExpect(MockMvcResultMatchers.status().isNoContent)
        Assertions.assertNotNull(userRoleRepository.findByUsernameAndRole("test_user_role", "ROLE_USER"))
        // duplicated add
        mockMvc
            .perform(
                MockMvcRequestBuilders.post("/user/addUserRole")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(gson.toJson(UserRoleRequest("test_user_role", "ROLE_USER")))
            )
            .andExpect(MockMvcResultMatchers.status().isConflict)
        // remove role
        mockMvc
            .perform(
                MockMvcRequestBuilders.delete("/user/removeUserRole")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(gson.toJson(UserRoleRequest("test_user_role", "ROLE_USER")))
            )
            .andExpect(MockMvcResultMatchers.status().isNoContent)
        // duplicated remove
        mockMvc
            .perform(
                MockMvcRequestBuilders.delete("/user/removeUserRole")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(gson.toJson(UserRoleRequest("test_user_role", "ROLE_USER")))
            )
            .andExpect(MockMvcResultMatchers.status().isNotFound)
    }

    @WithMockUser(username = "test_user_admin", roles = ["ADMIN"])
    @Test
    fun testAddUserRoleUserNotFound() {
        userRepository.deleteByUsername("test_user_404")
        mockMvc
            .perform(
                MockMvcRequestBuilders.post("/user/addUserRole")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(gson.toJson(UserRoleRequest("test_user_404", "ROLE_USER")))
            )
            .andExpect(MockMvcResultMatchers.status().isNotFound)
    }

    @WithMockUser(username = "test_user_admin", roles = ["ADMIN"])
    @Test
    fun testAddUserRoleUserInvalidRole() {
        mockMvc
            .perform(
                MockMvcRequestBuilders.post("/user/addUserRole")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(gson.toJson(UserRoleRequest("test_user_404", "ROLE_USER1")))
            )
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
    }

    @WithMockUser(username = "test_user_admin", roles = ["ADMIN"])
    @Test
    fun testDeleteUserCorrect() {
        userRepository.save(
            UserEntity(
                "test_user_deleted",
                passwordEncoder.encode("123456")
            )
        )
        mockMvc
            .perform(
                MockMvcRequestBuilders.delete("/user/deleteUser")
                    .param("username", "test_user_deleted")
            )
            .andExpect(MockMvcResultMatchers.status().isNoContent)
        Assertions.assertNull(userRepository.findByUsername("test_user_deleted"))
    }

    @WithMockUser(username = "test_user_admin", roles = ["ADMIN"])
    @Test
    fun testDeleteUserNotFound() {
        userRepository.deleteByUsername("test_user_404")
        mockMvc
            .perform(
                MockMvcRequestBuilders.delete("/user/deleteUser")
                    .param("username", "test_user_404")
            )
            .andExpect(MockMvcResultMatchers.status().isNotFound)
    }

    @WithMockUser(username = "test_user_admin", roles = ["ADMIN"])
    @Test
    fun testCreateUser() {
        userRepository.deleteByUsername("test_user_new")
        mockMvc
            .perform(
                MockMvcRequestBuilders.post("/user/createUser")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(gson.toJson(CreateUserRequest("test_user_new", "123456")))
            )
            .andExpect(MockMvcResultMatchers.status().isNoContent)
        Assertions.assertEquals(UserEntity.Status.DISABLED, userRepository.findByUsername("test_user_new")!!.status)
        Assertions.assertEquals(listOf<Any>(), userRoleRepository.findAllByUsername("test_user_new"))
        mockMvc
            .perform(
                MockMvcRequestBuilders.post("/user/createUser")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(gson.toJson(CreateUserRequest("test_user_new", "123456")))
            )
            .andExpect(MockMvcResultMatchers.status().isConflict)
    }

    @WithMockUser(username = "test_user_admin", roles = ["ADMIN"])
    @Test
    fun testCreateUserWrongName() {
        userRepository.deleteByUsername("0test_user_new")
        mockMvc
            .perform(
                MockMvcRequestBuilders.post("/user/createUser")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(gson.toJson(CreateUserRequest("0test_user_new", "123456")))
            )
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
        Assertions.assertNull(userRepository.findByUsername("0test_user_new"))
    }

    @WithMockUser(username = "test_user_change_status", roles = ["USER"])
    @Test
    fun testChangeStatusSelf() {
        userRepository.save(
            UserEntity(
                "test_user_change_status",
                passwordEncoder.encode("123456"),
                UserEntity.Status.ENABLED
            )
        )
        mockMvc
            .perform(
                MockMvcRequestBuilders.post("/user/changeStatus")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        gson.toJson(
                            UserChangeStatusRequest(
                                "test_user_change_status",
                                UserEntity.Status.DISABLED
                            )
                        )
                    )
            )
            .andExpect(MockMvcResultMatchers.status().isNoContent)
        Assertions.assertEquals(
            UserEntity.Status.DISABLED,
            userRepository.findByUsername("test_user_change_status")!!.status
        )
    }

    @WithMockUser(username = "test_user_admin", roles = ["ADMIN"])
    @Test
    fun testChangeStatusAdmin() {
        userRepository.save(
            UserEntity(
                "test_user_change_status",
                passwordEncoder.encode("123456"),
                UserEntity.Status.ENABLED
            )
        )
        mockMvc
            .perform(
                MockMvcRequestBuilders.post("/user/changeStatus")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        gson.toJson(
                            UserChangeStatusRequest(
                                "test_user_change_status",
                                UserEntity.Status.DISABLED
                            )
                        )
                    )
            )
            .andExpect(MockMvcResultMatchers.status().isNoContent)
        Assertions.assertEquals(
            UserEntity.Status.DISABLED,
            userRepository.findByUsername("test_user_change_status")!!.status
        )
    }

    @WithMockUser(username = "test_user", roles = ["USER"])
    @Test
    fun testChangeStatusNoPermission() {
        mockMvc
            .perform(
                MockMvcRequestBuilders.post("/user/changeStatus")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        gson.toJson(
                            UserChangeStatusRequest(
                                "test_user_404",
                                UserEntity.Status.DISABLED
                            )
                        )
                    )
            )
            .andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @WithMockUser(username = "test_user_change_password", roles = ["USER"])
    @Test
    fun testChangePasswordSelf() {
        userRepository.save(
            UserEntity(
                "test_user_change_password",
                passwordEncoder.encode("123456"),
                UserEntity.Status.ENABLED
            )
        )
        mockMvc
            .perform(
                MockMvcRequestBuilders.post("/user/changePassword")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        gson.toJson(
                            UserChangePasswordRequest(
                                "test_user_change_password",
                                "654321"
                            )
                        )
                    )
            )
            .andExpect(MockMvcResultMatchers.status().isNoContent)
        Assertions.assertTrue(
            passwordEncoder.matches(
                "654321",
                userRepository.findByUsername("test_user_change_password")!!.password
            )
        )
    }

    @WithMockUser(username = "test_user_admin", roles = ["ADMIN"])
    @Test
    fun testChangePasswordAdmin() {
        userRepository.save(
            UserEntity(
                "test_user_change_password",
                passwordEncoder.encode("123456"),
                UserEntity.Status.ENABLED
            )
        )
        mockMvc
            .perform(
                MockMvcRequestBuilders.post("/user/changePassword")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        gson.toJson(
                            UserChangePasswordRequest(
                                "test_user_change_password",
                                "654321"
                            )
                        )
                    )
            )
            .andExpect(MockMvcResultMatchers.status().isNoContent)
        Assertions.assertTrue(
            passwordEncoder.matches(
                "654321",
                userRepository.findByUsername("test_user_change_password")!!.password
            )
        )
    }

    @WithMockUser(username = "test_user", roles = ["USER"])
    @Test
    fun testChangePasswordNoPermission() {
        mockMvc
            .perform(
                MockMvcRequestBuilders.post("/user/changePassword")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        gson.toJson(
                            UserChangePasswordRequest(
                                "test_user_404",
                                "123456",
                            )
                        )
                    )
            )
            .andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @WithMockUser(username = "test_user", roles = ["USER"])
    @Test
    fun testListUserRoleCorrect() {
        assertDoesNotThrow<ResultActions> {
            mockMvc
                .perform(
                    MockMvcRequestBuilders.get("/user/listUserRoles")
                        .param("username", "test_user_controller_user")
                        .param("page", "0")
                        .param("size", "20")
                        .param("sort", "role,desc")
                )
                .andExpect(MockMvcResultMatchers.status().isOk)
        }
            .andExpect(MockMvcResultMatchers.jsonPath("$.length()", Matchers.not(0)))
    }

    @WithMockUser(username = "test_user", roles = ["USER"])
    @Test
    fun testListUserRolesWrongSortProperty() {
        assertDoesNotThrow<ResultActions> {
            mockMvc
                .perform(
                    MockMvcRequestBuilders.get("/user/listUserRoles")
                        .param("username", "something")
                        .param("page", "-10")
                        .param("size", "-99")
                        .param("sort", "username,desc")
                )
                .andExpect(MockMvcResultMatchers.status().isBadRequest)
        }
    }

    @WithMockUser(username = "test_user", roles = ["USER"])
    @Test
    fun testListUserRolesNotFound() {
        assertDoesNotThrow<ResultActions> {
            mockMvc
                .perform(
                    MockMvcRequestBuilders.get("/user/listUserRoles")
                        .param("username", "test_user_404")
                )
                .andExpect(MockMvcResultMatchers.status().isOk)
        }
            .andExpect(MockMvcResultMatchers.jsonPath("$.length()", Matchers.equalTo(0)))
    }

    @WithMockUser(username = "test_user", roles = ["USER"])
    @Test
    fun testQueryUserFound() {
        val resp = mockMvc
            .perform(
                MockMvcRequestBuilders.get("/user/queryUser")
                    .param("username", "test_user_controller_user")
            )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn().response.contentAsString
        val (username, status, roles) = gson.fromJson(resp, UserDetailModel::class.java)
        Assertions.assertEquals("test_user_controller_user", username)
        Assertions.assertEquals(UserEntity.Status.ENABLED, status)
        Assertions.assertEquals(listOf("ROLE_USER"), roles)
    }

    @WithMockUser(username = "test_user", roles = ["USER"])
    @Test
    fun testQueryUserNotFound() {
        mockMvc
            .perform(
                MockMvcRequestBuilders.get("/user/queryUser")
                    .param("username", "test_user_404")
            )
            .andExpect(MockMvcResultMatchers.status().isNotFound)
    }

    @WithMockUser(username = "test_user", roles = ["USER"])
    @Test
    fun testListUsernameCorrect() {
        assertDoesNotThrow<ResultActions> {
            mockMvc
                .perform(
                    MockMvcRequestBuilders.get("/user/listUsername")
                        .param("keyword", "something")
                        .param("page", "1")
                        .param("size", "20")
                        .param("sort", "username,desc")
                )
                .andExpect(MockMvcResultMatchers.status().isOk)
        }
    }

    @WithMockUser(username = "test_user", roles = ["USER"])
    @Test
    fun testListUsernameWrongSortProperty() {
        assertDoesNotThrow<ResultActions> {
            mockMvc
                .perform(
                    MockMvcRequestBuilders.get("/user/listUsername")
                        .param("keyword", "something")
                        .param("page", "-10")
                        .param("size", "-99")
                        .param("sort", "password,desc")
                )
                .andExpect(MockMvcResultMatchers.status().isBadRequest)
        }
    }
}
