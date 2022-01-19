package info.skyblond.archivedag.arstue

import info.skyblond.archivedag.arstue.entity.CertEntity
import info.skyblond.archivedag.arstue.model.CertDetailModel
import info.skyblond.archivedag.arstue.model.CertSigningResult
import info.skyblond.archivedag.arstue.repo.CertRepository
import info.skyblond.archivedag.arstue.service.CertSigningConfigService
import info.skyblond.archivedag.commons.DuplicatedEntityException
import info.skyblond.archivedag.commons.EntityNotFoundException
import info.skyblond.archivedag.commons.getUnixTimestamp
import org.bouncycastle.asn1.ASN1Sequence
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x500.style.BCStyle
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo
import org.bouncycastle.cert.X509v3CertificateBuilder
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder
import org.bouncycastle.crypto.util.PrivateKeyFactory
import org.bouncycastle.operator.DefaultDigestAlgorithmIdentifierFinder
import org.bouncycastle.operator.DefaultSignatureAlgorithmIdentifierFinder
import org.bouncycastle.operator.bc.BcRSAContentSignerBuilder
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.AuthenticationException
import org.springframework.stereotype.Service
import java.math.BigInteger
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.sql.Timestamp
import java.util.*
import javax.transaction.Transactional

@Service
class CertService(
    private val certSigningConfigService: CertSigningConfigService,
    private val certRepository: CertRepository
) {
    // TODO Using CSR instead of generate key
    // TODO Support ECC and RSA
    private val secureRandom = SecureRandom()

    private fun generateKeyPair(): KeyPair {
        val keyPairGenerator = KeyPairGenerator.getInstance("RSA", "BC")
        keyPairGenerator.initialize(certSigningConfigService.generatedKeySize, secureRandom)
        return keyPairGenerator.generateKeyPair()
    }

    private fun signKeyPair(userKeyPair: KeyPair, subject: X500Name): X509Certificate {
        val issuer = JcaX509CertificateHolder(certSigningConfigService.caCert).subject
        var serial: BigInteger
        do { // generate serial number
            serial = BigInteger.valueOf(secureRandom.nextLong())
            serial = serial.shiftLeft(java.lang.Long.BYTES * 8)
            serial = serial.or(BigInteger.valueOf(System.currentTimeMillis()))
            serial = serial.shiftLeft(java.lang.Long.BYTES * 8)
            serial = serial.or(BigInteger.valueOf(System.nanoTime()))
            serial = serial.abs()
        } while (certRepository.existsBySerialNumber(serial.toString(16)))
        val currentTimeStamp = System.currentTimeMillis()
        val notBefore = Date(currentTimeStamp)
        val notAfter = Date(
            currentTimeStamp + certSigningConfigService.expire.toMillis()
        )
        val publicKeyInfo = SubjectPublicKeyInfo.getInstance(
            ASN1Sequence.getInstance(userKeyPair.public.encoded)
        )
        val certBuilder = X509v3CertificateBuilder(
            issuer, serial, notBefore, notAfter, subject, publicKeyInfo
        )
        val sigAlgId = DefaultSignatureAlgorithmIdentifierFinder().find(certSigningConfigService.signAlgName)
        val digAlgId = DefaultDigestAlgorithmIdentifierFinder().find(sigAlgId)
        val contentSigner = BcRSAContentSignerBuilder(sigAlgId, digAlgId)
            .build(PrivateKeyFactory.createKey(certSigningConfigService.caPrivateKey.encoded))
        val certHolder = certBuilder.build(contentSigner)
        val converter = JcaX509CertificateConverter().setProvider("BC")
        return converter.getCertificate(certHolder)
    }

    @Transactional
    fun signCert(username: String): CertSigningResult {
        val userKeyPair = generateKeyPair()
        val subjectDN = certSigningConfigService.newX500NameBuilder()
            .addRDN(BCStyle.CN, username).build()
        val cert = signKeyPair(userKeyPair, subjectDN)

        // add cert to database
        val entity = CertEntity(
            cert.serialNumber.toString(16),
            username,
            Timestamp(cert.notBefore.time),
            Timestamp(cert.notAfter.time)
        )
        // No lock, might fail since at the time this cert is generating
        // the same serial number might be taken
        if (certRepository.existsBySerialNumber(entity.serialNumber)) {
            throw DuplicatedEntityException("Cert#" + entity.serialNumber)
        }
        certRepository.save(entity)
        return CertSigningResult(cert, userKeyPair.private)
    }

    fun listCertSerialNumber(
        blurOwner: Boolean, owner: String, issueStart: Timestamp, issueEnd: Timestamp,
        expireStart: Timestamp, expireEnd: Timestamp, pageable: Pageable
    ): List<String> {
        val queryResult: Page<CertEntity> = if (blurOwner) {
            certRepository.findAllByUsernameContainingAndIssuedTimeBetweenAndExpiredTimeBetween(
                owner, issueStart, issueEnd, expireStart, expireEnd, pageable
            )
        } else {
            certRepository.findAllByUsernameAndIssuedTimeBetweenAndExpiredTimeBetween(
                owner, issueStart, issueEnd, expireStart, expireEnd, pageable
            )
        }
        val result: MutableList<String> = LinkedList()
        queryResult.forEach { result.add(it.serialNumber) }
        return result
    }

    fun userOwnCert(username: String, certSerialNumber: String): Boolean {
        return certRepository.existsBySerialNumberAndUsername(certSerialNumber, username)
    }

    fun queryCert(serialNumber: String): CertDetailModel {
        val entity = certRepository.findBySerialNumber(serialNumber)
            ?: throw EntityNotFoundException("Cert#$serialNumber")
        return CertDetailModel(
            entity.serialNumber, entity.username,
            getUnixTimestamp(entity.issuedTime.time),
            getUnixTimestamp(entity.expiredTime.time),
            entity.status
        )
    }

    @Transactional
    fun changeCertStatus(
        serialNumber: String, newStatus: CertEntity.Status
    ) {
        if (!certRepository.existsBySerialNumber(serialNumber)) {
            throw EntityNotFoundException("Cert#$serialNumber")
        }
        certRepository.updateCertStatus(serialNumber, newStatus)
    }

    @Transactional
    fun deleteCert(serialNumber: String) {
        if (!certRepository.existsBySerialNumber(serialNumber)) {
            throw EntityNotFoundException("Cert#$serialNumber")
        }
        certRepository.deleteBySerialNumber(serialNumber)
    }

    fun verifyCertStatus(serialNumber: String, username: String) {
        // check username matches the cert
        if (!userOwnCert(username, serialNumber)) {
            throw object : BadCredentialsException("You don't own this certification.") {}
        }
        // get cert entity
        val entity = certRepository.findBySerialNumber(serialNumber)
            ?: throw object : BadCredentialsException("Cert#$serialNumber not found") {}
        when (entity.status) {
            CertEntity.Status.ENABLED -> {}
            CertEntity.Status.DISABLED -> throw object : AuthenticationException("Certification disabled") {}
            CertEntity.Status.REVOKED -> throw object : AuthenticationException("Certification revoked") {}
            CertEntity.Status.LOCKED -> throw object : AuthenticationException("Certification locked") {}
        }
    }
}
