package info.skyblond.archivedag.arstue.service

import info.skyblond.archivedag.arstue.config.CertSigningProperties
import info.skyblond.archivedag.commons.service.EtcdConfigService
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo
import org.bouncycastle.asn1.x500.X500NameBuilder
import org.bouncycastle.asn1.x500.style.BCStyle
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.openssl.PEMParser
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.security.PrivateKey
import java.security.Security
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.time.Duration

/**
 * Read config from etcd and provide those configs to other service.
 * */
@Service
class CertSigningConfigService(
    private val config: EtcdConfigService,
    private val certSigningProperties: CertSigningProperties
) {
    private val logger = LoggerFactory.getLogger(CertSigningConfigService::class.java)
    private val etcdNamespace = "arstue/user_cert"

    private val caCertPemEtcdConfigKey = "ca_cert_pem"
    private val caPrivateKeyPemEtcdConfigKey = "ca_private_key_pem"

    private val generatedKeySizeEtcdConfigKey = "generated_key_size"
    private val defaultGeneratedKeySize: Int = 4096

    private val signAlgNameEtcdConfigKey = "sign_alg_name"
    private val defaultSignAlgName = "SHA512WITHRSA"

    private val expireInDayEtcdConfigKey = "expire_in_day"
    private val defaultExpireInDay: Int = 90

    init {
        if (Security.getProvider("BC") == null) {
            Security.addProvider(BouncyCastleProvider())
        }
        logger.info(
            "Cert signing CA subject: ${
                parseCertPem(
                    config.requireString(etcdNamespace, caCertPemEtcdConfigKey)
                ).subjectX500Principal.name
            }"
        )
        logger.info(
            "Cert signing CA private key: ${
                parsePrivateKeyPem(
                    config.requireString(
                        etcdNamespace,
                        caPrivateKeyPemEtcdConfigKey
                    )
                ).encoded.size
            } bytes"
        )
        logger.info(
            "Cert generated key size: ${
                config.getInt(etcdNamespace, generatedKeySizeEtcdConfigKey, defaultGeneratedKeySize)
            }"
        )
        logger.info(
            "Cert sign alg name: ${
                config.getString(etcdNamespace, signAlgNameEtcdConfigKey, defaultSignAlgName)
            }"
        )
        logger.info(
            "Cert expire time: ${
                config.getInt(etcdNamespace, expireInDayEtcdConfigKey, defaultExpireInDay)
            } days"
        )
    }

    private fun parseCertPem(pem: String): X509Certificate {
        val factory = CertificateFactory.getInstance("X.509", "BC")
        return pem.byteInputStream(Charsets.UTF_8)
            .use { inputStream -> factory.generateCertificate(inputStream) as X509Certificate }
    }

    private fun parsePrivateKeyPem(pem: String): PrivateKey {
        val pemParser = PEMParser(pem.reader())
        val obj = pemParser.readObject()
        val converter = JcaPEMKeyConverter().setProvider("BC")
        // here we read CA private key
        val privateKey: PrivateKey = when (obj) {
            is PrivateKeyInfo -> {
                converter.getPrivateKey(obj)
            }
            else -> {
                throw RuntimeException("Unknown pem object: " + obj.javaClass.canonicalName)
            }
        }
        pemParser.close()
        return privateKey
    }

    val caCert: X509Certificate
        get() = parseCertPem(config.requireString(etcdNamespace, caCertPemEtcdConfigKey))

    val caPrivateKey: PrivateKey
        get() = parsePrivateKeyPem(config.requireString(etcdNamespace, caPrivateKeyPemEtcdConfigKey))

    val generatedKeySize: Int
        get() = config.getInt(etcdNamespace, generatedKeySizeEtcdConfigKey, defaultGeneratedKeySize)

    val signAlgName: String
        get() = config.getString(etcdNamespace, signAlgNameEtcdConfigKey, defaultSignAlgName)

    val expire: Duration
        get() = Duration.ofDays(config.getInt(etcdNamespace, expireInDayEtcdConfigKey, defaultExpireInDay).toLong())

    fun newX500NameBuilder(): X500NameBuilder {
        val builder = X500NameBuilder()
        if (!certSigningProperties.subjectDnC.isNullOrBlank()) {
            builder.addRDN(BCStyle.C, certSigningProperties.subjectDnC)
        }
        if (!certSigningProperties.subjectDnO.isNullOrBlank()) {
            builder.addRDN(BCStyle.O, certSigningProperties.subjectDnO)
        }
        if (!certSigningProperties.subjectDnOU.isNullOrBlank()) {
            builder.addRDN(BCStyle.OU, certSigningProperties.subjectDnOU)
        }
        return builder
    }
}
