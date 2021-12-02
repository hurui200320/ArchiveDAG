package info.skyblond.archivedag.controller;

import info.skyblond.archivedag.config.EmbeddedRedisConfiguration;
import info.skyblond.archivedag.config.WebMvcConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@SpringBootTest(
        classes = {EmbeddedRedisConfiguration.class, WebMvcConfig.class}
//        properties = {
//                "grpc.server.inProcessName=testUserController",
//                "grpc.client.inProcess.address=in-process:testUserController"
//        }
)
@ActiveProfiles("test")
@AutoConfigureMockMvc
public class UserControllerTest {
    @Autowired
    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
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

    @WithMockUser(username = "test_user", roles = "")
    @Test
    void testListUsernameNoPermission() {
        Assertions.assertDoesNotThrow(() -> this.mockMvc.perform(get("/user/listUsername")
                        .param("keyword", "something")
                        .param("page", "1")
                        .param("size", "20")
                        .param("sort", "username,desc"))
                .andExpect(MockMvcResultMatchers.status().isForbidden()));
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
