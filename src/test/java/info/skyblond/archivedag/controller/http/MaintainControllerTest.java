package info.skyblond.archivedag.controller.http;

import com.google.gson.Gson;
import info.skyblond.archivedag.config.EmbeddedRedisConfiguration;
import info.skyblond.archivedag.config.WebMvcConfig;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest(
        classes = {EmbeddedRedisConfiguration.class, WebMvcConfig.class}
)
@ActiveProfiles("test")
@AutoConfigureMockMvc
class MaintainControllerTest {
    @Autowired
    MockMvc mockMvc;
    @Autowired
    Gson gson;

    @WithMockUser(username = "test_user_admin", roles = "ADMIN")
    @Test
    void testConfig() throws Exception {
        this.mockMvc.perform(post("/maintain/updateConfig")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.gson.toJson(Map.of(
                                "test-maintain-controller-key", "value"
                        ))))
                .andExpect(MockMvcResultMatchers.status().isNoContent());
        this.mockMvc.perform(get("/maintain/listConfig")
                        .param("prefix", "test-maintain-controller"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.test-maintain-controller-key",
                        Matchers.equalTo("value")));
    }
}
