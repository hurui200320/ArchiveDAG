package info.skyblond.archivedag.arudaz.service

import info.skyblond.archivedag.arudaz.model.ProtoReceipt
import info.skyblond.archivedag.commons.service.EncryptionService
import info.skyblond.archivedag.commons.service.EtcdConfigService
import io.ipfs.multihash.Multihash
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.*

@Service
class ProtoReceiptService(
    private val configService: EtcdConfigService,
    private val encryptionService: EncryptionService
) {
    private val logger = LoggerFactory.getLogger(ProtoReceiptService::class.java)

    private val etcdNamespace = "arudaz/proto_receipt"
    private val encryptionKeyEtcdConfigKey = "encryption_key_bytearray"

    private fun getEncryptionKey(): ByteArray {
        val result = configService.getByteArray(etcdNamespace, encryptionKeyEtcdConfigKey)
        if (result == null) {
            logger.warn("No proto receipt encryption key found, generating one...")
            val key = encryptionService.newKey()
            configService.setByteArray(etcdNamespace, encryptionKeyEtcdConfigKey, key)
            return key
        }
        return result
    }

    fun encryptReceipt(receipt: ProtoReceipt): String {
        val content = "${receipt.recordId},${receipt.username},${receipt.primaryHash.toBase58()}"
        return encryptionService.encrypt(content, getEncryptionKey())
    }

    fun decryptReceipt(encryptedReceipt: String): ProtoReceipt? {
        return try {
            val content = encryptionService.decryptToString(encryptedReceipt, getEncryptionKey()).split(",")
            if (content.size != 3) return null
            return ProtoReceipt(UUID.fromString(content[0]), content[1], Multihash.fromBase58(content[2]))
        } catch (t: Throwable) {
            logger.debug("Failed to decrypt receipt", t)
            null
        }
    }
}
