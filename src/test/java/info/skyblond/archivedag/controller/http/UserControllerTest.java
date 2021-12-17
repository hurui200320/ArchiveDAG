package info.skyblond.archivedag.controller.http;

import com.google.gson.Gson;
import info.skyblond.archivedag.config.EmbeddedRedisConfiguration;
import info.skyblond.archivedag.config.WebMvcConfig;
import info.skyblond.archivedag.model.ao.CreateUserRequest;
import info.skyblond.archivedag.model.ao.UserChangePasswordRequest;
import info.skyblond.archivedag.model.ao.UserChangeStatusRequest;
import info.skyblond.archivedag.model.ao.UserRoleRequest;
import info.skyblond.archivedag.model.bo.UserDetailModel;
import info.skyblond.archivedag.model.entity.UserEntity;
import info.skyblond.archivedag.model.entity.UserRoleEntity;
import info.skyblond.archivedag.repo.UserRepository;
import info.skyblond.archivedag.repo.UserRoleRepository;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

@SpringBootTest(
        classes = {EmbeddedRedisConfiguration.class, WebMvcConfig.class}
)
@ActiveProfiles("test")
@AutoConfigureMockMvc
public class UserControllerTest {
    @Autowired
    MockMvc mockMvc;

    @Autowired
    PasswordEncoder passwordEncoder;
    @Autowired
    UserRepository userRepository;
    @Autowired
    UserRoleRepository userRoleRepository;
    @Autowired
    Gson gson;

    @BeforeEach
    void setUp() {
        this.userRepository.save(new UserEntity("test_user_controller_user",
                this.passwordEncoder.encode("password"),
                UserEntity.Status.ENABLED));
        this.userRoleRepository.save(new UserRoleEntity("test_user_controller_user", "ROLE_USER"));
    }

    @WithMockUser(username = "test_user_admin", roles = "ADMIN")
    @Test
    void testRemoveUserRoleUserNotFound() throws Exception {
        this.userRepository.deleteByUsername("test_user_404");
        this.mockMvc.perform(delete("/user/removeUserRole")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.gson.toJson(new UserRoleRequest(
                                "test_user_404", "ROLE_USER"
                        ))))
                .andExpect(MockMvcResultMatchers.status().isNotFound());
    }


    @WithMockUser(username = "test_user_admin", roles = "ADMIN")
    @Test
    void testAddAndRemoveUserRole() throws Exception {
        this.userRepository.save(new UserEntity("test_user_role",
                this.passwordEncoder.encode("123456")));
        this.userRoleRepository.deleteAllByUsername("test_user_role");
        // Add role
        this.mockMvc.perform(post("/user/addUserRole")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.gson.toJson(new UserRoleRequest(
                                "test_user_role", "ROLE_USER"
                        ))))
                .andExpect(MockMvcResultMatchers.status().isNoContent());
        assertNotNull(this.userRoleRepository.findByUsernameAndRole("test_user_role", "ROLE_USER"));
        // duplicated add
        this.mockMvc.perform(post("/user/addUserRole")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.gson.toJson(new UserRoleRequest(
                                "test_user_role", "ROLE_USER"
                        ))))
                .andExpect(MockMvcResultMatchers.status().isConflict());
        // remove role
        this.mockMvc.perform(delete("/user/removeUserRole")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.gson.toJson(new UserRoleRequest(
                                "test_user_role", "ROLE_USER"
                        ))))
                .andExpect(MockMvcResultMatchers.status().isNoContent());
        // duplicated remove
        this.mockMvc.perform(delete("/user/removeUserRole")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.gson.toJson(new UserRoleRequest(
                                "test_user_role", "ROLE_USER"
                        ))))
                .andExpect(MockMvcResultMatchers.status().isNotFound());
    }

    @WithMockUser(username = "test_user_admin", roles = "ADMIN")
    @Test
    void testAddUserRoleUserNotFound() throws Exception {
        this.userRepository.deleteByUsername("test_user_404");
        this.mockMvc.perform(post("/user/addUserRole")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.gson.toJson(new UserRoleRequest(
                                "test_user_404", "ROLE_USER"
                        ))))
                .andExpect(MockMvcResultMatchers.status().isNotFound());
    }

    @WithMockUser(username = "test_user_admin", roles = "ADMIN")
    @Test
    void testAddUserRoleUserInvalidRole() throws Exception {
        this.mockMvc.perform(post("/user/addUserRole")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.gson.toJson(new UserRoleRequest(
                                "test_user_404", "ROLE_USER1"
                        ))))
                .andExpect(MockMvcResultMatchers.status().isBadRequest());
    }


    @WithMockUser(username = "test_user_admin", roles = "ADMIN")
    @Test
    void testDeleteUserCorrect() throws Exception {
        this.userRepository.save(new UserEntity("test_user_deleted",
                this.passwordEncoder.encode("123456")));
        this.mockMvc.perform(delete("/user/deleteUser")
                        .param("username", "test_user_deleted"))
                .andExpect(MockMvcResultMatchers.status().isNoContent());
        assertNull(this.userRepository.findByUsername("test_user_deleted"));
    }

    @WithMockUser(username = "test_user_admin", roles = "ADMIN")
    @Test
    void testDeleteUserNotFound() throws Exception {
        this.userRepository.deleteByUsername("test_user_404");
        this.mockMvc.perform(delete("/user/deleteUser")
                        .param("username", "test_user_404"))
                .andExpect(MockMvcResultMatchers.status().isNotFound());
    }


    @WithMockUser(username = "test_user_admin", roles = "ADMIN")
    @Test
    void testCreateUser() throws Exception {
        this.userRepository.deleteByUsername("test_user_new");
        this.mockMvc.perform(post("/user/createUser")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.gson.toJson(new CreateUserRequest(
                                "test_user_new", "123456"
                        ))))
                .andExpect(MockMvcResultMatchers.status().isNoContent());
        assertEquals(UserEntity.Status.LOCKED, this.userRepository.findByUsername("test_user_new").getStatus());
        assertEquals(List.of(), this.userRoleRepository.findAllByUsername("test_user_new"));
        this.mockMvc.perform(post("/user/createUser")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.gson.toJson(new CreateUserRequest(
                                "test_user_new", "123456"
                        ))))
                .andExpect(MockMvcResultMatchers.status().isConflict());
    }

    @WithMockUser(username = "test_user_admin", roles = "ADMIN")
    @Test
    void testCreateUserWrongName() throws Exception {
        this.userRepository.deleteByUsername("0test_user_new");
        this.mockMvc.perform(post("/user/createUser")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.gson.toJson(new CreateUserRequest(
                                "0test_user_new", "123456"
                        ))))
                .andExpect(MockMvcResultMatchers.status().isBadRequest());
        assertNull(this.userRepository.findByUsername("0test_user_new"));
    }


    @WithMockUser(username = "test_user_change_status", roles = "USER")
    @Test
    void testChangeStatusSelf() throws Exception {
        this.userRepository.save(new UserEntity("test_user_change_status",
                this.passwordEncoder.encode("123456"),
                UserEntity.Status.ENABLED));
        this.mockMvc.perform(post("/user/changeStatus")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.gson.toJson(new UserChangeStatusRequest(
                                "test_user_change_status", "123456", UserEntity.Status.DISABLED
                        ))))
                .andExpect(MockMvcResultMatchers.status().isNoContent());
        assertEquals(UserEntity.Status.DISABLED, this.userRepository.findByUsername("test_user_change_status").getStatus());
    }

    @WithMockUser(username = "test_user_admin", roles = "ADMIN")
    @Test
    void testChangeStatusAdmin() throws Exception {
        this.userRepository.save(new UserEntity("test_user_change_status",
                this.passwordEncoder.encode("123456"),
                UserEntity.Status.ENABLED));
        this.mockMvc.perform(post("/user/changeStatus")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.gson.toJson(new UserChangeStatusRequest(
                                "test_user_change_status", "123456", UserEntity.Status.DISABLED
                        ))))
                .andExpect(MockMvcResultMatchers.status().isNoContent());
        assertEquals(UserEntity.Status.DISABLED, this.userRepository.findByUsername("test_user_change_status").getStatus());
    }

    @WithMockUser(username = "test_user_admin", roles = "ADMIN")
    @Test
    void testChangeStatusWrongPassword() throws Exception {
        this.userRepository.save(new UserEntity("test_user_change_status",
                this.passwordEncoder.encode("123456"),
                UserEntity.Status.ENABLED));
        this.mockMvc.perform(post("/user/changeStatus")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.gson.toJson(new UserChangeStatusRequest(
                                "test_user_change_status", "1123456", UserEntity.Status.DISABLED
                        ))))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized());
    }

    @WithMockUser(username = "test_user", roles = "USER")
    @Test
    void testChangeStatusNoPermission() throws Exception {
        this.mockMvc.perform(post("/user/changeStatus")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.gson.toJson(new UserChangeStatusRequest(
                                "test_user_404", "123456", UserEntity.Status.DISABLED
                        ))))
                .andExpect(MockMvcResultMatchers.status().isForbidden());
    }


    @WithMockUser(username = "test_user_change_password", roles = "USER")
    @Test
    void testChangePasswordSelf() throws Exception {
        this.userRepository.save(new UserEntity("test_user_change_password",
                this.passwordEncoder.encode("123456"),
                UserEntity.Status.ENABLED));
        this.mockMvc.perform(post("/user/changePassword")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.gson.toJson(new UserChangePasswordRequest(
                                "test_user_change_password", "123456", "654321"
                        ))))
                .andExpect(MockMvcResultMatchers.status().isNoContent());
        assertTrue(this.passwordEncoder.matches("654321", this.userRepository.findByUsername("test_user_change_password").getPassword()));
    }

    @WithMockUser(username = "test_user_admin", roles = "ADMIN")
    @Test
    void testChangePasswordAdmin() throws Exception {
        this.userRepository.save(new UserEntity("test_user_change_password",
                this.passwordEncoder.encode("123456"),
                UserEntity.Status.ENABLED));
        this.mockMvc.perform(post("/user/changePassword")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.gson.toJson(new UserChangePasswordRequest(
                                "test_user_change_password", "123456", "654321"
                        ))))
                .andExpect(MockMvcResultMatchers.status().isNoContent());
        assertTrue(this.passwordEncoder.matches("654321", this.userRepository.findByUsername("test_user_change_password").getPassword()));
    }

    @WithMockUser(username = "test_user_admin", roles = "ADMIN")
    @Test
    void testChangePasswordWrongPassword() throws Exception {
        this.userRepository.save(new UserEntity("test_user_change_password",
                this.passwordEncoder.encode("123456"),
                UserEntity.Status.ENABLED));
        this.mockMvc.perform(post("/user/changePassword")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.gson.toJson(new UserChangePasswordRequest(
                                "test_user_change_password", "1123456", "654321"
                        ))))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized());
    }

    @WithMockUser(username = "test_user", roles = "USER")
    @Test
    void testChangePasswordNoPermission() throws Exception {
        this.mockMvc.perform(post("/user/changePassword")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.gson.toJson(new UserChangePasswordRequest(
                                "test_user_404", "123456", "654321"
                        ))))
                .andExpect(MockMvcResultMatchers.status().isForbidden());
    }


    @WithMockUser(username = "test_user", roles = "USER")
    @Test
    void testListUserRoleCorrect() throws Exception {
        Assertions.assertDoesNotThrow(() -> this.mockMvc.perform(get("/user/listUserRoles")
                                .param("username", "test_user_controller_user")
                                .param("page", "0")
                                .param("size", "20")
                                .param("sort", "role,desc"))
                        .andExpect(MockMvcResultMatchers.status().isOk()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.length()", Matchers.not(0)));
    }

    @WithMockUser(username = "test_user", roles = "USER")
    @Test
    void testListUserRolesWrongSortProperty() {
        Assertions.assertDoesNotThrow(() -> this.mockMvc.perform(get("/user/listUserRoles")
                        .param("username", "something")
                        .param("page", "-10")
                        .param("size", "-99")
                        .param("sort", "username,desc"))
                .andExpect(MockMvcResultMatchers.status().isBadRequest()));
    }

    @WithMockUser(username = "test_user", roles = "USER")
    @Test
    void testListUserRolesNotFound() throws Exception {
        Assertions.assertDoesNotThrow(() -> this.mockMvc.perform(get("/user/listUserRoles")
                                .param("username", "test_user_404"))
                        .andExpect(MockMvcResultMatchers.status().isOk()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.length()", Matchers.equalTo(0)));
    }


    @WithMockUser(username = "test_user", roles = "USER")
    @Test
    void testQueryUserFound() throws Exception {
        String resp = this.mockMvc.perform(get("/user/queryUser")
                        .param("username", "test_user_controller_user"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn().getResponse().getContentAsString();
        UserDetailModel user = this.gson.fromJson(resp, UserDetailModel.class);
        assertEquals("test_user_controller_user", user.getUsername());
        assertEquals(UserEntity.Status.ENABLED, user.getStatus());
        assertEquals(List.of("ROLE_USER"), user.getRoles());
    }

    @WithMockUser(username = "test_user", roles = "USER")
    @Test
    void testQueryUserNotFound() throws Exception {
        String resp = this.mockMvc.perform(get("/user/queryUser")
                        .param("username", "test_user_404"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertNull(this.gson.fromJson(resp, UserDetailModel.class));
    }


    @WithMockUser(username = "test_user", roles = "USER")
    @Test
    void testListUsernameCorrect() {
        Assertions.assertDoesNotThrow(() -> this.mockMvc.perform(get("/user/listUsername")
                        .param("keyword", "something")
                        .param("page", "1")
                        .param("size", "20")
                        .param("sort", "username,desc"))
                .andExpect(MockMvcResultMatchers.status().isOk()));
    }

    @WithMockUser(username = "test_user", roles = "USER")
    @Test
    void testListUsernameWrongSortProperty() {
        Assertions.assertDoesNotThrow(() -> this.mockMvc.perform(get("/user/listUsername")
                        .param("keyword", "something")
                        .param("page", "-10")
                        .param("size", "-99")
                        .param("sort", "password,desc"))
                .andExpect(MockMvcResultMatchers.status().isBadRequest()));
    }
}
