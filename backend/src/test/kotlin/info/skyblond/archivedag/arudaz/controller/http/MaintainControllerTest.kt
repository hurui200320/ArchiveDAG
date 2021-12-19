package info.skyblond.archivedag.arudaz.controller.http

import com.google.gson.Gson
import info.skyblond.archivedag.ariteg.config.EmbeddedRedisConfiguration
import org.hamcrest.Matchers
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers

@SpringBootTest(classes = [EmbeddedRedisConfiguration::class])
@ActiveProfiles("test")
@AutoConfigureMockMvc
internal class MaintainControllerTest {
    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var gson: Gson

    @WithMockUser(username = "test_user_admin", roles = ["ADMIN"])
    @Test
    fun testConfig() {
        mockMvc
            .perform(
                MockMvcRequestBuilders.post("/maintain/updateConfig")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        gson.toJson(
                            mapOf(
                                "test-maintain-controller-key" to "value"
                            )
                        )
                    )
            )
            .andExpect(MockMvcResultMatchers.status().isNoContent)
        mockMvc
            .perform(
                MockMvcRequestBuilders.get("/maintain/listConfig")
                    .param("prefix", "test-maintain-controller")
            )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                MockMvcResultMatchers.jsonPath(
                    "$.test-maintain-controller-key",
                    Matchers.equalTo("value")
                )
            )
    }
}
