package info.skyblond.archivedag.config;

import info.skyblond.archivedag.model.CertSigningInfo;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.openssl.PEMDecryptorProvider;
import org.bouncycastle.openssl.PEMEncryptedKeyPair;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcePEMDecryptorProviderBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

@Configuration
@EnableConfigurationProperties(CertSigningProperties.class)
public class CertSigningConfig {
    private final CertSigningProperties properties;

    public CertSigningConfig(CertSigningProperties properties) {
        this.properties = properties;
    }

    private X509Certificate readX509Cert(ReadableByteChannel channel) throws CertificateException, NoSuchProviderException, IOException {
        CertificateFactory fact = CertificateFactory.getInstance("X.509", "BC");
        try (
                InputStream inputStream = Channels.newInputStream(channel)
        ) {
            return (X509Certificate) fact.generateCertificate(inputStream);
        }
    }

    private PrivateKey readPrivateKey(ReadableByteChannel channel, String password) throws IOException {
        PEMParser pemParser = new PEMParser(Channels.newReader(channel, StandardCharsets.UTF_8));
        Object object = pemParser.readObject();
        JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider("BC");
        // here we read CA private key
        PrivateKey privateKey;
        if (object instanceof PEMEncryptedKeyPair) {
            // Encrypted key - we will use provided password
            PEMEncryptedKeyPair ckp = (PEMEncryptedKeyPair) object;
            PEMDecryptorProvider decProv = new JcePEMDecryptorProviderBuilder()
                    .setProvider("BC").build(password.toCharArray());
            privateKey = converter.getKeyPair(ckp.decryptKeyPair(decProv)).getPrivate();
        } else if (object instanceof PrivateKeyInfo) {
            // actually read
            PrivateKeyInfo privateKeyInfo = (PrivateKeyInfo) object;
            privateKey = converter.getPrivateKey(privateKeyInfo);
        } else if (object instanceof PEMKeyPair) {
            // Unencrypted key - no password needed
            PEMKeyPair ukp = (PEMKeyPair) object;
            privateKey = converter.getKeyPair(ukp).getPrivate();
        } else {
            throw new RuntimeException("Unknown read result: " + object.getClass().getCanonicalName());
        }
        pemParser.close();
        return privateKey;
    }

    @Bean
    public CertSigningInfo certSigningInfo() throws IOException, CertificateException, NoSuchProviderException {
        PrivateKey caPrivateKey = this.readPrivateKey(
                this.properties.getCaPrivateKey().readableChannel(),
                this.properties.getCaPrivateKeyPassword());
        X509Certificate caCert = this.readX509Cert(this.properties.getCaCert().readableChannel());

        return new CertSigningInfo(caPrivateKey, caCert, this.properties.getGenerateKeySize());
    }
}

