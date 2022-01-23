package info.skyblond.archivedag.arstue.service

import info.skyblond.archivedag.arstue.config.CertSigningProperties
import info.skyblond.archivedag.commons.service.EtcdSimpleConfigService
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

@Service
class CertSigningConfigService(
    private val config: EtcdSimpleConfigService,
    private val certSigningProperties: CertSigningProperties
) {
    private val logger = LoggerFactory.getLogger(CertSigningConfigService::class.java)
    private val etcdConfigPrefix = "/application/arstue/config/user_cert/"

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
                    config.requireConfig(
                        etcdConfigPrefix,
                        caCertPemEtcdConfigKey
                    )
                ).subjectX500Principal.name
            }"
        )
        logger.info(
            "Cert signing CA private key: ${
                parsePrivateKeyPem(
                    config.requireConfig(
                        etcdConfigPrefix,
                        caPrivateKeyPemEtcdConfigKey
                    )
                ).encoded.size
            } bytes"
        )
        logger.info(
            "Cert generated key size: ${
                getInt(generatedKeySizeEtcdConfigKey, defaultGeneratedKeySize)
            }"
        )
        logger.info(
            "Cert sign alg name: ${
                getString(signAlgNameEtcdConfigKey, defaultSignAlgName)
            }"
        )
        logger.info(
            "Cert expire time: ${
                getInt(expireInDayEtcdConfigKey, defaultExpireInDay)
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

    private fun setInt(key: String, int: Int) {
        config.putConfig(etcdConfigPrefix, key, int.toString())
    }

    private fun getInt(key: String, default: Int): Int {
        val text = config.getConfig(etcdConfigPrefix, key)
        if (text == null) {
            logger.warn(
                "Config: ${
                    config.getStringKey(etcdConfigPrefix, key)
                } not found, use default value: $default"
            )
            setInt(key, default)
            return default
        }
        return text.toInt()
    }

    private fun setString(key: String, str: String) {
        config.putConfig(etcdConfigPrefix, key, str)
    }

    private fun getString(key: String, default: String): String {
        val text = config.getConfig(etcdConfigPrefix, key)
        if (text == null) {
            logger.warn(
                "Config: ${
                    config.getStringKey(etcdConfigPrefix, key)
                } not found, use default value: $default"
            )
            setString(key, default)
            return default
        }
        return text
    }

    val caCert: X509Certificate
        get() = parseCertPem(config.requireConfig(etcdConfigPrefix, caCertPemEtcdConfigKey))

    val caPrivateKey: PrivateKey
        get() = parsePrivateKeyPem(config.requireConfig(etcdConfigPrefix, caPrivateKeyPemEtcdConfigKey))

    val generatedKeySize: Int
        get() = getInt(generatedKeySizeEtcdConfigKey, defaultGeneratedKeySize)

    val signAlgName: String
        get() = getString(signAlgNameEtcdConfigKey, defaultSignAlgName)

    val expire: Duration
        get() = Duration.ofDays(getInt(expireInDayEtcdConfigKey, defaultExpireInDay).toLong())

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
