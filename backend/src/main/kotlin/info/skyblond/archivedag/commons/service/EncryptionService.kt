package info.skyblond.archivedag.commons.service

import org.bouncycastle.crypto.engines.AESEngine
import org.bouncycastle.crypto.modes.GCMBlockCipher
import org.bouncycastle.crypto.params.AEADParameters
import org.bouncycastle.crypto.params.KeyParameter
import org.springframework.stereotype.Service
import java.security.SecureRandom
import java.util.*

/**
 * AES-256-GCM service using BouncyCastle.
 * */
@Service
class EncryptionService {
    private val secureRandom = SecureRandom()

    // Pre-configured Encryption Parameters
    private final val nonceByteSize = 96 / Byte.SIZE_BITS
    private final val macBitSize = 128
    private final val keyByteSize = 256 / Byte.SIZE_BITS

    // Base64 helpers
    private fun encodeToBase64(source: ByteArray): String {
        return Base64.getEncoder().encodeToString(source)
    }

    private fun decodeFromBase64(base64: String): ByteArray {
        return Base64.getDecoder().decode(base64)
    }

    fun newKeyBase64(): String {
        val result = ByteArray(keyByteSize)
        secureRandom.nextBytes(result)
        return encodeToBase64(result)
    }

    fun decodeKeyBase64(base64Key: String): ByteArray {
        val result = decodeFromBase64(base64Key)
        require(result.size == keyByteSize) { "Wrong key size. Expected $keyByteSize bytes, actual ${result.size} bytes" }
        return result
    }

    private fun newNonce(): ByteArray {
        val result = ByteArray(nonceByteSize)
        secureRandom.nextBytes(result)
        return result
    }

    private fun decodeNonceBase64(base64Nonce: String): ByteArray {
        val result = decodeFromBase64(base64Nonce)
        require(result.size == nonceByteSize) { "Wrong nonce size. Expected $nonceByteSize bytes, actual ${result.size} bytes" }
        return result
    }

    private fun encryptInternal(source: ByteArray, key: ByteArray, nonce: ByteArray): ByteArray {
        val cipher = GCMBlockCipher(AESEngine())
        val parameters = AEADParameters(KeyParameter(key), macBitSize, nonce)
        cipher.init(true, parameters)

        val encryptedBytes = ByteArray(cipher.getOutputSize(source.size))
        val retLen = cipher.processBytes(source, 0, source.size, encryptedBytes, 0)
        cipher.doFinal(encryptedBytes, retLen)
        return encryptedBytes
    }

    fun encrypt(source: ByteArray, key: ByteArray): String {
        val nonce = newNonce()
        val encryptedSource = encryptInternal(source, key, nonce)
        return "${encodeToBase64(nonce)}.${encodeToBase64(encryptedSource)}"
    }

    fun encrypt(source: String, key: ByteArray): String {
        return encrypt(source.encodeToByteArray(), key)
    }

    private fun decryptInternal(source: ByteArray, key: ByteArray, nonce: ByteArray): ByteArray {
        val cipher = GCMBlockCipher(AESEngine())
        val parameters = AEADParameters(KeyParameter(key), macBitSize, nonce)
        cipher.init(false, parameters)

        val plainBytes = ByteArray(cipher.getOutputSize(source.size))
        val retLen = cipher.processBytes(source, 0, source.size, plainBytes, 0)
        cipher.doFinal(plainBytes, retLen)
        return plainBytes
    }

    fun decrypt(source: String, key: ByteArray): ByteArray {
        val t = source.split(".")
        require(t.size == 2) { "Invalid encrypted string" }
        val nonce = decodeNonceBase64(t[0])
        val encryptedContent = decodeFromBase64(t[1])
        return decryptInternal(encryptedContent, key, nonce)
    }

    fun decryptToString(source: String, key: ByteArray): String {
        return String(decrypt(source, key), Charsets.UTF_8)
    }

}
