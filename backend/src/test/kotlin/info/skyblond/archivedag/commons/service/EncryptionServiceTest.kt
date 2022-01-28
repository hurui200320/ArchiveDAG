package info.skyblond.archivedag.commons.service

import org.bouncycastle.crypto.InvalidCipherTextException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class EncryptionServiceTest {
    private val encryptionService = EncryptionService()

    @Test
    fun testNormalOperation() {
        val key = encryptionService.newKey()
        val text = "0123456789\n" +
                "A fox jumps over the lazy dog\n" +
                "UTF-8编码测试\n日本語"

        val encrypted = encryptionService.encrypt(text, key)
        val decrypted = encryptionService.decryptToString(encrypted, key)
        assertEquals(text, decrypted)
    }

    @Test
    fun testEncryptWithInvalidKey() {
        assertThrows<IllegalArgumentException> { encryptionService.encrypt(ByteArray(0), ByteArray(6)) }
    }

    @Test
    fun testDecryptWithInvalidKey() {
        val key1 = encryptionService.newKey()
        val encrypted = encryptionService.encrypt("0123456789", key1)

        val key2 = encryptionService.newKey()
        // mac check in GCM failed
        assertThrows<InvalidCipherTextException> { encryptionService.decryptToString(encrypted, key2) }
    }

    @Test
    fun testDecryptInvalidSource() {
        val key = encryptionService.newKey()
        assertThrows<IllegalArgumentException> { encryptionService.decrypt("some.thing", key) }
        assertThrows<IllegalArgumentException> { encryptionService.decrypt("something", key) }
        assertThrows<IllegalArgumentException> { encryptionService.decrypt("AAAAAA==.AAAAAA==", key) }
    }
}
