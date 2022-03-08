package info.skyblond.archivedag.arudaz.controller.http

import com.google.gson.Gson
import info.skyblond.archivedag.arstue.CertService
import info.skyblond.archivedag.arstue.entity.CertEntity
import info.skyblond.archivedag.arstue.repo.CertRepository
import info.skyblond.archivedag.arudaz.model.CertChangeStatusRequest
import org.hamcrest.Matchers
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
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
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import javax.transaction.Transactional

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
internal class CertControllerTest {
    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var certRepository: CertRepository

    @Autowired
    lateinit var gson: Gson

    @Autowired
    lateinit var certService: CertService

    @BeforeEach
    @Transactional
    internal fun setUp() {
        certRepository.deleteAllByUsername("test_user_404")
    }

    @WithMockUser(username = "test_user", roles = ["USER"])
    @Test
    fun testDeleteCertUser() {
        val serialNumber = generateCert("test_user")
        Assertions.assertTrue(certRepository.existsBySerialNumber(serialNumber))
        mockMvc
            .perform(
                MockMvcRequestBuilders.delete("/cert/deleteCert")
                    .param("serial_number", serialNumber)
            )
            .andExpect(MockMvcResultMatchers.status().isNoContent)
        Assertions.assertFalse(certRepository.existsBySerialNumber(serialNumber))
        mockMvc.perform(
            MockMvcRequestBuilders.delete("/cert/deleteCert")
                .param("serial_number", "serialNumber")
        )
            .andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @WithMockUser(username = "test_user_admin", roles = ["ADMIN"])
    @Test
    fun testDeleteCertAdmin() {
        val serialNumber = generateCert("test_user")
        Assertions.assertTrue(certRepository.existsBySerialNumber(serialNumber))
        mockMvc
            .perform(
                MockMvcRequestBuilders.delete("/cert/deleteCert")
                    .param("serial_number", serialNumber)
            )
            .andExpect(MockMvcResultMatchers.status().isNoContent)
        Assertions.assertFalse(certRepository.existsBySerialNumber(serialNumber))
    }

    @WithMockUser(username = "test_user", roles = ["USER"])
    @Test
    fun testChangeCertStatusUser() {
        val serialNumber = generateCert("test_user")
        mockMvc
            .perform(
                MockMvcRequestBuilders.post("/cert/changeCertStatus")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        gson.toJson(
                            CertChangeStatusRequest(
                                serialNumber, CertEntity.Status.ENABLED
                            )
                        )
                    )
            )
            .andExpect(MockMvcResultMatchers.status().isNoContent)
        mockMvc
            .perform(
                MockMvcRequestBuilders.get("/cert/queryCert")
                    .param("serial_number", serialNumber)
            )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.serialNumber", Matchers.equalTo(serialNumber)))
            .andExpect(MockMvcResultMatchers.jsonPath("$.owner", Matchers.equalTo("test_user")))
            .andExpect(MockMvcResultMatchers.jsonPath("$.status", Matchers.equalTo("ENABLED")))
        mockMvc
            .perform(
                MockMvcRequestBuilders.post("/cert/changeCertStatus")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        gson.toJson(
                            CertChangeStatusRequest(
                                "serialNumber", CertEntity.Status.ENABLED
                            )
                        )
                    )
            )
            .andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @WithMockUser(username = "test_user_admin", roles = ["ADMIN"])
    @Test
    fun testChangeCertStatusAdmin() {
        val serialNumber = generateCert("test_user")
        mockMvc
            .perform(
                MockMvcRequestBuilders.post("/cert/changeCertStatus")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        gson.toJson(
                            CertChangeStatusRequest(
                                serialNumber, CertEntity.Status.ENABLED
                            )
                        )
                    )
            )
            .andExpect(MockMvcResultMatchers.status().isNoContent)
        mockMvc
            .perform(
                MockMvcRequestBuilders.get("/cert/queryCert")
                    .param("serial_number", serialNumber)
            )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.serialNumber", Matchers.equalTo(serialNumber)))
            .andExpect(MockMvcResultMatchers.jsonPath("$.owner", Matchers.equalTo("test_user")))
            .andExpect(MockMvcResultMatchers.jsonPath("$.status", Matchers.equalTo("ENABLED")))
    }

    @WithMockUser(username = "test_user", roles = ["USER"])
    @Test
    fun testQueryCertUser() {
        val serialNumber = generateCert("test_user")
        mockMvc
            .perform(
                MockMvcRequestBuilders.get("/cert/queryCert")
                    .param("serial_number", serialNumber)
            )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.serialNumber", Matchers.equalTo(serialNumber)))
            .andExpect(MockMvcResultMatchers.jsonPath("$.owner", Matchers.equalTo("test_user")))
            .andExpect(MockMvcResultMatchers.jsonPath("$.status", Matchers.equalTo("LOCKED")))
        mockMvc
            .perform(
                MockMvcRequestBuilders.get("/cert/queryCert")
                    .param("serial_number", generateCert("test_user_admin"))
            )
            .andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @WithMockUser(username = "test_user_admin", roles = ["ADMIN"])
    @Test
    fun testQueryCertAdmin() {
        val serialNumber = generateCert("test_user")
        mockMvc
            .perform(
                MockMvcRequestBuilders.get("/cert/queryCert")
                    .param("serial_number", serialNumber)
            )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.serialNumber", Matchers.equalTo(serialNumber)))
            .andExpect(MockMvcResultMatchers.jsonPath("$.owner", Matchers.equalTo("test_user")))
            .andExpect(MockMvcResultMatchers.jsonPath("$.status", Matchers.equalTo("LOCKED")))
    }

    @WithMockUser(username = "test_user", roles = ["USER"])
    @Test
    fun testListCertSerialNumberUser() {
        generateCert("test_user")
        mockMvc
            .perform(
                MockMvcRequestBuilders.get("/cert/listCertSerialNumber")
                    .param("owner", "")
            )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.length()", Matchers.not(0)))
        mockMvc
            .perform(
                MockMvcRequestBuilders.get("/cert/listCertSerialNumber")
                    .param("owner", "test_user_404")
            )
            .andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @WithMockUser(username = "test_user_admin", roles = ["ADMIN"])
    @Test
    fun testListCertSerialNumberAdmin() {
        mockMvc
            .perform(
                MockMvcRequestBuilders.get("/cert/listCertSerialNumber")
                    .param("owner", "test_user_404")
            )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.length()", Matchers.equalTo(0)))
    }

    @WithMockUser(username = "test_user", roles = ["USER"])
    @Test
    fun testSignNewCert() {
        val body = mockMvc.perform(MockMvcRequestBuilders.get("/cert/signNewCert"))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn().response.contentAsString

        @Suppress("UNCHECKED_CAST")
        val result: Map<String, String> = gson.fromJson(body, MutableMap::class.java) as Map<String, String>
        val certificate = readX509Cert(result["cert"]!!)

        // make sure it appears in database
        Assertions.assertTrue(certRepository.existsBySerialNumber(certificate.serialNumber.toString(16)))
    }

    private fun readX509Cert(pem: String): X509Certificate {
        val factory = CertificateFactory.getInstance("X.509", "BC")
        return pem.byteInputStream(Charsets.UTF_8)
            .use { inputStream -> factory.generateCertificate(inputStream) as X509Certificate }
    }

    private fun generateCert(username: String): String {
        return Assertions.assertDoesNotThrow<String> {
            certService.signCert(username).certificate.serialNumber.toString(
                16
            )
        }
    }
}
