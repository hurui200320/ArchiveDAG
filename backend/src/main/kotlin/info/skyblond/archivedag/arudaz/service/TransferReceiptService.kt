package info.skyblond.archivedag.arudaz.service

import info.skyblond.archivedag.arudaz.model.TransferReceipt
import info.skyblond.archivedag.commons.service.EncryptionService
import info.skyblond.archivedag.commons.service.EtcdConfigService
import io.ipfs.multihash.Multihash
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.*

@Service
class TransferReceiptService(
    private val configService: EtcdConfigService,
    private val encryptionService: EncryptionService
) {
    private val logger = LoggerFactory.getLogger(TransferReceiptService::class.java)

    private val etcdNamespace = "arudaz/proto_receipt"
    private val encryptionKeyEtcdConfigKey = "encryption_key_bytearray"

    @Synchronized
    private fun generateEncryptionKey(): ByteArray {
        val result = configService.getByteArray(etcdNamespace, encryptionKeyEtcdConfigKey)
        if (result != null) return result
        logger.warn("No proto receipt encryption key found, generating one...")
        val key = encryptionService.newKey()
        configService.setByteArray(etcdNamespace, encryptionKeyEtcdConfigKey, key)
        return key
    }

    private fun getEncryptionKey(): ByteArray {
        return configService.getByteArray(etcdNamespace, encryptionKeyEtcdConfigKey) ?: return generateEncryptionKey()
    }

    fun encryptReceipt(receipt: TransferReceipt): String {
        val content = "${receipt.recordId},${receipt.username},${receipt.primaryHash.toBase58()}"
        return encryptionService.encrypt(content, getEncryptionKey())
    }

    fun decryptReceipt(encryptedReceipt: String): TransferReceipt? {
        return try {
            val content = encryptionService.decryptToString(encryptedReceipt, getEncryptionKey()).split(",")
            if (content.size != 3) return null
            return TransferReceipt(UUID.fromString(content[0]), content[1], Multihash.fromBase58(content[2]))
        } catch (t: Throwable) {
            logger.debug("Failed to decrypt receipt", t)
            null
        }
    }
}
