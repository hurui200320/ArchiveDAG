package info.skyblond.archivedag.ariteg.storage

import info.skyblond.archivedag.ariteg.model.AritegObject
import info.skyblond.archivedag.ariteg.model.StorageStatus
import info.skyblond.archivedag.ariteg.model.StoreReceipt
import info.skyblond.archivedag.ariteg.protos.AritegLink
import info.skyblond.archivedag.ariteg.protos.AritegObjectType
import info.skyblond.archivedag.ariteg.utils.toMultihashBase58
import io.ipfs.multihash.Multihash
import org.slf4j.LoggerFactory
import java.io.File
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.function.BiFunction

class AritegFileStorageService(
    primaryProviderType: Multihash.Type,
    secondaryProviderType: Multihash.Type,
    baseDir: File,
    threadNum: Int,
    queueSize: Int
) : AritegFileAbstractStorage(primaryProviderType, secondaryProviderType, baseDir), AutoCloseable {
    private val logger = LoggerFactory.getLogger(AritegFileStorageService::class.java)

    private val threadPool: ThreadPoolExecutor = ThreadPoolExecutor(
        threadNum, threadNum, 0L, TimeUnit.MILLISECONDS,
        LinkedBlockingQueue(queueSize), ThreadPoolExecutor.CallerRunsPolicy()
    )

    init {
        logger.info("Using {} threads and {} queue slot", threadNum, queueSize)
    }

    override fun doWrite(primaryMultihash: Multihash, type: AritegObjectType, rawBytes: ByteArray) {
        writeToFile(primaryMultihash, type, rawBytes)
    }

    override fun store(
        name: String,
        proto: AritegObject,
        checkBeforeWrite: BiFunction<Multihash, Multihash, Boolean>
    ): StoreReceipt {
        return storeInternal(
            name, proto.toProto().toByteArray(),
            proto.getObjectType(), checkBeforeWrite, threadPool
        )
    }

    override fun queryStatus(link: AritegLink): StorageStatus? {
        return queryFile(link)
    }

    override fun restoreLink(link: AritegLink) {
        // no need to restore on disk
    }

    override fun loadProto(link: AritegLink): AritegObject {
        return loadFromFile(link)
            ?: throw IllegalStateException("Cannot load ${link.toMultihashBase58()}: not found")
    }

    override fun deleteProto(link: AritegLink): Boolean {
        return deleteFile(link)
    }

    override fun close() {
        threadPool.shutdown()
        while (!threadPool.awaitTermination(1, TimeUnit.MINUTES)) {
            logger.trace("Waiting termination...")
        }
    }
}
