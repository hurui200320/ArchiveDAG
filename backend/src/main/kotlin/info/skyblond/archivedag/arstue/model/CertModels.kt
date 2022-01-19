package info.skyblond.archivedag.arstue.model

import info.skyblond.archivedag.arstue.entity.CertEntity
import java.security.PrivateKey
import java.security.cert.X509Certificate

data class CertSigningResult(
    var certificate: X509Certificate,
    var privateKey: PrivateKey,
)

data class CertDetailModel(
    val serialNumber: String,
    val owner: String,
    val issuedTimestamp: Long,
    val expiredTimestamp: Long,
    val status: CertEntity.Status
)
