package info.skyblond.archivedag.controller;

import com.google.gson.Gson;
import info.skyblond.archivedag.model.JWTAuthResponse;
import info.skyblond.archivedag.model.JwtAuthRequest;
import info.skyblond.archivedag.model.entity.UserEntity;
import info.skyblond.archivedag.model.entity.UserRoleEntity;
import info.skyblond.archivedag.repo.UserRepository;
import info.skyblond.archivedag.repo.UserRoleRepository;
import info.skyblond.archivedag.security.JwtTokenManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.util.NestedServletException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;


@SpringBootTest
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    AuthController authController;
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
        this.mockMvc = MockMvcBuilders.standaloneSetup(this.authController).build();
        this.userRepository.save(new UserEntity("test_user_jwt",
                this.passwordEncoder.encode("password"),
                UserEntity.UserStatus.ENABLED));
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
    void testWrongPassword() {
        assertThrows(BadCredentialsException.class, () -> {
            try {
                this.mockMvc.perform(post("/public/auth")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.gson.toJson(new JwtAuthRequest(
                                "test_user_jwt",
                                "not_password"))));
            } catch (NestedServletException e) {
                throw e.getCause();
            }
        });
    }
}
