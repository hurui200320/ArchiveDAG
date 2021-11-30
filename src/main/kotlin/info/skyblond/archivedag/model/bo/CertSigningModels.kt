package info.skyblond.archivedag.model.bo

import java.security.PrivateKey
import java.security.cert.X509Certificate

data class CertSigningInfo(
    val caPrivateKey: PrivateKey,
    val caCert: X509Certificate,
    val generatedKeySize: Int,
)

data class CertSigningResult (
    var certificate: X509Certificate,
    var privateKey: PrivateKey,
)
