package info.skyblond.archivedag.ariteg.storage

import com.google.protobuf.ByteString
import info.skyblond.archivedag.ariteg.model.*
import info.skyblond.archivedag.ariteg.multihash.MultihashProvider
import info.skyblond.archivedag.ariteg.multihash.MultihashProviders
import info.skyblond.archivedag.ariteg.protos.*
import info.skyblond.archivedag.ariteg.utils.nop
import info.skyblond.archivedag.ariteg.utils.toMultihash
import info.skyblond.archivedag.ariteg.utils.toMultihashBase58
import info.skyblond.archivedag.commons.getUnixTimestamp
import io.ipfs.multihash.Multihash
import org.slf4j.LoggerFactory
import java.io.File
import java.util.concurrent.CompletableFuture
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.function.BiFunction

class AritegFileStorageService(
    private val primaryProvider: MultihashProvider,
    private val secondaryProvider: MultihashProvider,
    private val baseDir: File,
    threadNum: Int,
    queueSize: Int
) : AritegStorageService, AutoCloseable {
    private val logger = LoggerFactory.getLogger(AritegFileStorageService::class.java)

    private val threadPool: ThreadPoolExecutor = ThreadPoolExecutor(
        threadNum, threadNum, 0L, TimeUnit.MILLISECONDS,
        LinkedBlockingQueue(queueSize), ThreadPoolExecutor.CallerRunsPolicy()
    )

    init {
        logger.info("Using base dir: {}", baseDir.canonicalPath)
    }

    private fun multihashToFileMapper(
        base: File, type: AritegObjectType, primaryHash: Multihash
    ): File {
        val typeDir = File(base, type.name.lowercase())
        if (typeDir.mkdirs()) {
            logger.trace("Create dir: " + typeDir.absolutePath)
        }
        return File(typeDir, primaryHash.toBase58())
    }

    private fun storeInternal(
        name: String,
        rawBytes: ByteArray,
        type: AritegObjectType,
        checkBeforeWrite: BiFunction<Multihash, Multihash, Boolean>
    ): StoreReceipt {
        // calculate multihash
        val primaryMultihash = primaryProvider.digest(rawBytes)
        val future = CompletableFuture.supplyAsync({
            // calculate secondary hash
            val secondaryMultihash = secondaryProvider.digest(rawBytes)
            // run the check, return if we get false
            if (checkBeforeWrite.apply(primaryMultihash, secondaryMultihash)) {
                // check pass, add request into queue
                val file = multihashToFileMapper(baseDir, type, primaryMultihash)
                logger.debug("Writing into file `{}`", file.canonicalPath)
                file.writeBytes(rawBytes)
                primaryMultihash
            } else {
                null
            }
        }, threadPool)

        return StoreReceipt(
            AritegLink.newBuilder()
                .setName(name)
                .setMultihash(ByteString.copyFrom(primaryMultihash.toBytes()))
                .setType(type)
                .build(),
            future
        )
    }

    override fun store(
        name: String,
        proto: AritegObject,
        checkBeforeWrite: BiFunction<Multihash, Multihash, Boolean>
    ): StoreReceipt {
        return storeInternal(
            name, proto.toProto().toByteArray(),
            proto.getObjectType(), checkBeforeWrite
        )
    }

    override fun queryStatus(link: AritegLink): StorageStatus? {
        val file = multihashToFileMapper(
            baseDir, link.type, link.multihash.toMultihash()
        )
        return if (file.exists()) {
            assert(file.length() <= Int.MAX_VALUE) { "File size overflow: ${file.length()}" }
            StorageStatus(
                getUnixTimestamp(),
                -1,
                file.length().toInt()
            )
        } else {
            null
        }
    }

    override fun restoreLinks(links: List<AritegLink>, option: RestoreOption?): CompletableFuture<Void> {
        // no need to restore on disk
        return CompletableFuture.runAsync { nop() }
    }

    override fun loadProto(link: AritegLink): AritegObject {
        val multihash = link.multihash.toMultihash()
        logger.info("Loading {}", multihash.toBase58())
        val file = multihashToFileMapper(baseDir, link.type, multihash)
        if (!file.exists())
            throw IllegalStateException("Cannot load ${link.toMultihashBase58()}: not found")
        file.inputStream().use {
            MultihashProviders.mustMatch(multihash, it)
        }
        return when (link.type!!) {
            AritegObjectType.BLOB -> file.inputStream().use {
                BlobObject.fromProto(AritegBlobObject.parseFrom(it))
            }
            AritegObjectType.LIST -> file.inputStream().use {
                ListObject.fromProto(AritegListObject.parseFrom(it))
            }
            AritegObjectType.TREE -> file.inputStream().use {
                TreeObject.fromProto(AritegTreeObject.parseFrom(it))
            }
            AritegObjectType.COMMIT -> file.inputStream().use {
                CommitObject.fromProto(AritegCommitObject.parseFrom(it))
            }
            else -> throw IllegalStateException("Invalid object type: ${link.type}")
        }
    }

    override fun deleteProto(link: AritegLink): Boolean {
        return multihashToFileMapper(
            baseDir, link.type, link.multihash.toMultihash()
        ).delete()
    }

    override fun close() {
        threadPool.shutdown()
        while (!threadPool.awaitTermination(1, TimeUnit.MINUTES)) {
            logger.trace("Waiting termination...")
        }
    }
}
