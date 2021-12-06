package info.skyblond.archivedag.util

import info.skyblond.archivedag.model.bo.CertSigningResult
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo
import org.bouncycastle.openssl.PEMEncryptedKeyPair
import org.bouncycastle.openssl.PEMKeyPair
import org.bouncycastle.openssl.PEMParser
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter
import org.bouncycastle.openssl.jcajce.JcaPEMWriter
import org.bouncycastle.openssl.jcajce.JcePEMDecryptorProviderBuilder
import org.bouncycastle.util.io.pem.PemObject
import java.io.ByteArrayOutputStream
import java.nio.channels.Channels
import java.nio.channels.ReadableByteChannel
import java.nio.charset.StandardCharsets
import java.security.PrivateKey
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate

fun getUnixTimestamp(): Long {
    return System.currentTimeMillis() / 1000
}

fun getUnixTimestamp(millis: Long): Long {
    return millis / 1000
}

fun writeSignCertToString(result: CertSigningResult): String {
    val outputStream = ByteArrayOutputStream()
    val pemWriter = JcaPEMWriter(outputStream.writer(Charsets.UTF_8))
    pemWriter.writeObject(result.certificate)
    pemWriter.close()
    return String(outputStream.toByteArray(), Charsets.UTF_8)
}

fun writeSignKeyToString(result: CertSigningResult): String {
    val outputStream = ByteArrayOutputStream()
    val pemWriter = JcaPEMWriter(outputStream.writer(Charsets.UTF_8))
    pemWriter.writeObject(PemObject("PRIVATE KEY", result.privateKey.encoded))
    pemWriter.close()
    return String(outputStream.toByteArray(), Charsets.UTF_8)
}

fun readX509Cert(pem: String): X509Certificate {
    return readX509Cert(Channels.newChannel(pem.byteInputStream(Charsets.UTF_8)))
}

fun readX509Cert(channel: ReadableByteChannel): X509Certificate {
    val fact = CertificateFactory.getInstance("X.509", "BC")
    Channels.newInputStream(channel)
        .use { inputStream -> return fact.generateCertificate(inputStream) as X509Certificate }
}


fun readPrivateKey(pem: String, password: String): PrivateKey {
    return readPrivateKey(
        Channels.newChannel(pem.byteInputStream(Charsets.UTF_8)),
        password
    )
}

fun readPrivateKey(channel: ReadableByteChannel, password: String): PrivateKey {
    val pemParser = PEMParser(Channels.newReader(channel, StandardCharsets.UTF_8))
    val obj = pemParser.readObject()
    val converter = JcaPEMKeyConverter().setProvider("BC")
    // here we read CA private key
    val privateKey: PrivateKey = when (obj) {
        is PEMEncryptedKeyPair -> {
            // Encrypted key - we will use provided password
            val decProv = JcePEMDecryptorProviderBuilder()
                .setProvider("BC").build(password.toCharArray())
            converter.getKeyPair(obj.decryptKeyPair(decProv)).private
        }
        is PrivateKeyInfo -> {
            // actually read
            converter.getPrivateKey(obj)
        }
        is PEMKeyPair -> {
            // Unencrypted key - no password needed
            converter.getKeyPair(obj).private
        }
        else -> {
            throw RuntimeException("Unknown read result: " + obj.javaClass.canonicalName)
        }
    }
    pemParser.close()
    return privateKey
}
