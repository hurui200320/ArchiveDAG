package info.skyblond.archivedag.arstue

import info.skyblond.archivedag.arstue.entity.CertEntity
import info.skyblond.archivedag.arstue.service.CertSigningConfigService
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.AuthenticationException
import org.springframework.test.context.ActiveProfiles
import java.security.KeyStore
import java.security.cert.X509Certificate
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

@SpringBootTest
@ActiveProfiles("test")
internal class CertServiceTest {

    @Autowired
    lateinit var certService: CertService

    @Autowired
    lateinit var certSigningInfo: CertSigningConfigService

    @Test
    fun testSignedCert() {
        val (_, cert, privateKey) = certService.signCert("test_user")

        // make sure private key matches the cert
        val rsaPrivateKey = privateKey as RSAPrivateKey
        val rsaPublicKey = cert.publicKey as RSAPublicKey
        assertEquals(rsaPrivateKey.modulus, rsaPublicKey.modulus)

        // then check the cert matches the CA
        val anchors = KeyStore.getInstance(KeyStore.getDefaultType())
        anchors.load(null, null)
        anchors.setCertificateEntry("cert", this.certSigningInfo.caCert)
        // set up our CA as trust anchors and init trust manager
        val trustManagerFactory = TrustManagerFactory.getInstance(
            TrustManagerFactory.getDefaultAlgorithm()
        )
        trustManagerFactory.init(anchors)
        for (trustManager in trustManagerFactory.trustManagers) {
            if (trustManager is X509TrustManager) {
                Assertions.assertDoesNotThrow {
                    trustManager.checkServerTrusted(arrayOf(cert), "RSA")
                }
            }
        }
    }

    private fun getSerialNum(certificate: X509Certificate): String {
        return certificate.serialNumber.toString(16)
    }

    @Test
    fun testVerifyCertStatus() {
        // test different status
        val disabled = certService.signCert("test_user").certificate
        certService.changeCertStatus(getSerialNum(disabled), CertEntity.Status.DISABLED)
        assertEquals(CertEntity.Status.DISABLED, certService.queryCert(getSerialNum(disabled)).status)
        val enabled = certService.signCert("test_user").certificate
        certService.changeCertStatus(getSerialNum(enabled), CertEntity.Status.ENABLED)
        assertEquals(CertEntity.Status.ENABLED, certService.queryCert(getSerialNum(enabled)).status)

        assertThrows(AuthenticationException::class.java) {
            certService.verifyCertStatus(getSerialNum(disabled), "test_user")
        }

        // test others cert
        val others = certService.signCert("test_user_404").certificate
        assertThrows(BadCredentialsException::class.java) {
            certService.verifyCertStatus(getSerialNum(others), "test_user")
        }

        // test normal cert
        Assertions.assertDoesNotThrow { certService.verifyCertStatus(getSerialNum(enabled), "test_user") }
    }

}
