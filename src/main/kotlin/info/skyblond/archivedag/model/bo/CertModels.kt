package info.skyblond.archivedag.model.bo

import info.skyblond.archivedag.model.entity.CertEntity
import info.skyblond.archivedag.util.getUnixTimestamp
import java.security.PrivateKey
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit

data class CertSigningInfo(
    val caPrivateKey: PrivateKey,
    val caCert: X509Certificate,
    val generatedKeySize: Int,
    val signAlgName: String,
    val customSubjectDN: String,
    val expireInDuration: Long,
    val expireInUnit: TimeUnit
)

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
