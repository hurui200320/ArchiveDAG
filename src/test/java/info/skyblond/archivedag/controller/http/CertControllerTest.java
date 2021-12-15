package info.skyblond.archivedag.controller.http;

import com.google.gson.Gson;
import info.skyblond.archivedag.config.EmbeddedRedisConfiguration;
import info.skyblond.archivedag.config.WebMvcConfig;
import info.skyblond.archivedag.model.ao.CertChangeStatusRequest;
import info.skyblond.archivedag.model.bo.CertSigningInfo;
import info.skyblond.archivedag.model.entity.CertEntity;
import info.skyblond.archivedag.repo.CertRepository;
import info.skyblond.archivedag.service.impl.CertService;
import info.skyblond.archivedag.util.GeneralKt;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

@SpringBootTest(
        classes = {EmbeddedRedisConfiguration.class, WebMvcConfig.class}
)
@ActiveProfiles("test")
@AutoConfigureMockMvc
class CertControllerTest {
    @Autowired
    MockMvc mockMvc;
    @Autowired
    CertRepository certRepository;
    @Autowired
    Gson gson;
    @Autowired
    CertSigningInfo certSigningInfo;
    @Autowired
    CertService certService;

    @BeforeEach
    void setUp() {
    }

    @WithMockUser(username = "test_user", roles = "USER")
    @Test
    void testDeleteCertUser() throws Exception {
        String serialNumber = this.generateCert("test_user");
        assertTrue(this.certRepository.existsBySerialNumber(serialNumber));
        this.mockMvc.perform(delete("/cert/deleteCert")
                        .param("serial_number", serialNumber))
                .andExpect(MockMvcResultMatchers.status().isNoContent());
        assertFalse(this.certRepository.existsBySerialNumber(serialNumber));

        this.mockMvc.perform(delete("/cert/deleteCert")
                        .param("serial_number", "serialNumber"))
                .andExpect(MockMvcResultMatchers.status().isForbidden());
    }

    @WithMockUser(username = "test_user_admin", roles = "ADMIN")
    @Test
    void testDeleteCertAdmin() throws Exception {
        String serialNumber = this.generateCert("test_user");
        assertTrue(this.certRepository.existsBySerialNumber(serialNumber));
        this.mockMvc.perform(delete("/cert/deleteCert")
                        .param("serial_number", serialNumber))
                .andExpect(MockMvcResultMatchers.status().isNoContent());
        assertFalse(this.certRepository.existsBySerialNumber(serialNumber));
    }


    @WithMockUser(username = "test_user", roles = "USER")
    @Test
    void testChangeCertStatusUser() throws Exception {
        String serialNumber = this.generateCert("test_user");
        this.mockMvc.perform(post("/cert/changeCertStatus")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.gson.toJson(new CertChangeStatusRequest(
                                serialNumber, CertEntity.Status.ENABLED
                        ))))
                .andExpect(MockMvcResultMatchers.status().isNoContent());
        this.mockMvc.perform(get("/cert/queryCert")
                        .param("serial_number", serialNumber))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.serialNumber", Matchers.equalTo(serialNumber)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.owner", Matchers.equalTo("test_user")))
                .andExpect(MockMvcResultMatchers.jsonPath("$.status", Matchers.equalTo("ENABLED")));

        this.mockMvc.perform(post("/cert/changeCertStatus")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.gson.toJson(new CertChangeStatusRequest(
                                "serialNumber", CertEntity.Status.ENABLED
                        ))))
                .andExpect(MockMvcResultMatchers.status().isForbidden());
    }

    @WithMockUser(username = "test_user_admin", roles = "ADMIN")
    @Test
    void testChangeCertStatusAdmin() throws Exception {
        String serialNumber = this.generateCert("test_user");
        this.mockMvc.perform(post("/cert/changeCertStatus")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.gson.toJson(new CertChangeStatusRequest(
                                serialNumber, CertEntity.Status.ENABLED
                        ))))
                .andExpect(MockMvcResultMatchers.status().isNoContent());
        this.mockMvc.perform(get("/cert/queryCert")
                        .param("serial_number", serialNumber))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.serialNumber", Matchers.equalTo(serialNumber)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.owner", Matchers.equalTo("test_user")))
                .andExpect(MockMvcResultMatchers.jsonPath("$.status", Matchers.equalTo("ENABLED")));
    }


    @WithMockUser(username = "test_user", roles = "USER")
    @Test
    void testQueryCertUser() throws Exception {
        String serialNumber = this.generateCert("test_user");
        this.mockMvc.perform(get("/cert/queryCert")
                        .param("serial_number", serialNumber))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.serialNumber", Matchers.equalTo(serialNumber)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.owner", Matchers.equalTo("test_user")))
                .andExpect(MockMvcResultMatchers.jsonPath("$.status", Matchers.equalTo("LOCKED")));

        this.mockMvc.perform(get("/cert/queryCert")
                        .param("serial_number", this.generateCert("test_user_admin")))
                .andExpect(MockMvcResultMatchers.status().isForbidden());
    }

    @WithMockUser(username = "test_user_admin", roles = "ADMIN")
    @Test
    void testQueryCertAdmin() throws Exception {
        String serialNumber = this.generateCert("test_user");
        this.mockMvc.perform(get("/cert/queryCert")
                        .param("serial_number", serialNumber))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.serialNumber", Matchers.equalTo(serialNumber)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.owner", Matchers.equalTo("test_user")))
                .andExpect(MockMvcResultMatchers.jsonPath("$.status", Matchers.equalTo("LOCKED")));
    }

    @WithMockUser(username = "test_user", roles = "USER")
    @Test
    void testListCertSerialNumberUser() throws Exception {
        this.generateCert("test_user");
        this.mockMvc.perform(get("/cert/listCertSerialNumber")
                        .param("owner", "test_user"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.length()", Matchers.not(0)));

        this.mockMvc.perform(get("/cert/listCertSerialNumber")
                        .param("owner", "test_user_404"))
                .andExpect(MockMvcResultMatchers.status().isForbidden());
    }

    @WithMockUser(username = "test_user_admin", roles = "ADMIN")
    @Test
    void testListCertSerialNumberAdmin() throws Exception {
        this.mockMvc.perform(get("/cert/listCertSerialNumber")
                        .param("owner", "test_user_404"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.length()", Matchers.equalTo(0)));
    }

    @SuppressWarnings("unchecked")
    @WithMockUser(username = "test_user", roles = "USER")
    @Test
    void testSignNewCert() throws Exception {
        String body = this.mockMvc.perform(get("/cert/signNewCert"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn().getResponse().getContentAsString();
        Map<String, String> result = this.gson.fromJson(body, Map.class);
        PrivateKey privateKey = GeneralKt.readPrivateKey(result.get("private_key"), "");
        X509Certificate certificate = GeneralKt.readX509Cert(result.get("cert"));

        // make sure private key matches the cert
        RSAPrivateKey rsaPrivateKey = (RSAPrivateKey) privateKey;
        RSAPublicKey rsaPublicKey = (RSAPublicKey) certificate.getPublicKey();
        assertEquals(rsaPrivateKey.getModulus(), rsaPublicKey.getModulus());

        // then check the cert matches the CA
        KeyStore anchors = KeyStore.getInstance(KeyStore.getDefaultType());
        anchors.load(null, null);
        anchors.setCertificateEntry("cert", this.certSigningInfo.getCaCert());
        // set up our CA as trust anchors and init trust manager
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(
                TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(anchors);
        for (TrustManager trustManager : trustManagerFactory.getTrustManagers()) {
            if (trustManager instanceof X509TrustManager) {
                X509TrustManager x509TrustManager = (X509TrustManager) trustManager;
                assertDoesNotThrow(() -> x509TrustManager.checkServerTrusted(
                        new X509Certificate[]{certificate}, "RSA"));
            }
        }

        // make sure it appears in database
        assertTrue(this.certRepository.existsBySerialNumber(certificate.getSerialNumber().toString(16)));
    }

    private String generateCert(String username) {
        return assertDoesNotThrow(() -> this.certService.signCert(username).getCertificate().getSerialNumber().toString(16));
    }

}
