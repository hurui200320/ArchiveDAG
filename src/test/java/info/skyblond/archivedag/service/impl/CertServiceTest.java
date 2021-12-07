package info.skyblond.archivedag.service.impl;

import info.skyblond.archivedag.model.entity.CertEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.test.context.ActiveProfiles;

import java.security.cert.X509Certificate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class CertServiceTest {

    @Autowired
    CertService certService;

    @Test
    void testVerifyCertStatus() throws Exception {
        X509Certificate others = this.certService.signCert("test_user_404").getCertificate();
        X509Certificate locked = this.certService.signCert("test_user").getCertificate();
        assertEquals(CertEntity.Status.LOCKED, this.certService.queryCert(getSerialNum(locked)).getStatus());
        X509Certificate revoked = this.certService.signCert("test_user").getCertificate();
        this.certService.changeCertStatus(getSerialNum(revoked), CertEntity.Status.REVOKED);
        assertEquals(CertEntity.Status.REVOKED, this.certService.queryCert(getSerialNum(revoked)).getStatus());
        X509Certificate disabled = this.certService.signCert("test_user").getCertificate();
        this.certService.changeCertStatus(getSerialNum(disabled), CertEntity.Status.DISABLED);
        assertEquals(CertEntity.Status.DISABLED, this.certService.queryCert(getSerialNum(disabled)).getStatus());
        X509Certificate enabled = this.certService.signCert("test_user").getCertificate();
        this.certService.changeCertStatus(getSerialNum(enabled), CertEntity.Status.ENABLED);
        assertEquals(CertEntity.Status.ENABLED, this.certService.queryCert(getSerialNum(enabled)).getStatus());

        assertThrows(BadCredentialsException.class,
                () -> this.certService.verifyCertStatus(getSerialNum(others), "test_user"));
        assertThrows(AuthenticationException.class,
                () -> this.certService.verifyCertStatus(getSerialNum(locked), "test_user"));
        assertThrows(AuthenticationException.class,
                () -> this.certService.verifyCertStatus(getSerialNum(revoked), "test_user"));
        assertThrows(AuthenticationException.class,
                () -> this.certService.verifyCertStatus(getSerialNum(disabled), "test_user"));
        assertDoesNotThrow(() -> this.certService.verifyCertStatus(getSerialNum(enabled), "test_user"));
    }

    private String getSerialNum(X509Certificate certificate) {
        return certificate.getSerialNumber().toString(16);
    }
}
