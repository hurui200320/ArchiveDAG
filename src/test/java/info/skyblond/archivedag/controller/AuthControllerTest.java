package info.skyblond.archivedag.controller;

import com.google.gson.Gson;
import info.skyblond.archivedag.model.bo.JWTAuthResponse;
import info.skyblond.archivedag.model.bo.JwtAuthRequest;
import info.skyblond.archivedag.model.entity.UserEntity;
import info.skyblond.archivedag.model.entity.UserRoleEntity;
import info.skyblond.archivedag.repo.UserRepository;
import info.skyblond.archivedag.repo.UserRoleRepository;
import info.skyblond.archivedag.security.JwtTokenManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;


@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class AuthControllerTest {

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
    @Autowired
    JwtTokenManager tokenManager;

    @BeforeEach
    void setUp() {
        this.userRepository.save(new UserEntity("test_user_jwt",
                this.passwordEncoder.encode("password"),
                UserEntity.Status.ENABLED));
        this.userRoleRepository.save(new UserRoleEntity("test_user_jwt", "ROLE_USER"));
    }

    @Test
    void testSignJwt() throws Exception {
        String resp = this.mockMvc.perform(post("/public/auth")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.gson.toJson(
                                new JwtAuthRequest("test_user_jwt",
                                        "password"))))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn().getResponse().getContentAsString();
        JWTAuthResponse jwtResponse = this.gson.fromJson(resp, JWTAuthResponse.class);
        assertEquals("test_user_jwt",
                this.tokenManager.getUsernameFromToken(jwtResponse.getToken()));
    }

    @Test
    void testWrongPassword() throws Exception {
        this.mockMvc.perform(post("/public/auth")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.gson.toJson(new JwtAuthRequest(
                                "test_user_jwt",
                                "not_password"))))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized());
    }

    @Test
    void testSpringSecurity() throws Exception {
        this.mockMvc.perform(get("/something_404"))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized());
    }
}
