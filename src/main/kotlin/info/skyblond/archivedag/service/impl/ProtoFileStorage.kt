package info.skyblond.archivedag.service.impl

import com.google.protobuf.ByteString
import info.skyblond.archivedag.model.bo.ProtoStatus
import info.skyblond.archivedag.model.bo.RestoreOption
import info.skyblond.archivedag.model.bo.StoreReceipt
import info.skyblond.archivedag.model.exception.IllegalObjectStatusException
import info.skyblond.archivedag.model.exception.StoreProtoException
import info.skyblond.archivedag.service.intf.ProtoStorageService
import info.skyblond.archivedag.util.CompletableFutureUtils
import info.skyblond.archivedag.util.getUnixTimestamp
import info.skyblond.ariteg.AritegLink
import info.skyblond.ariteg.AritegObject
import info.skyblond.ariteg.ObjectType
import info.skyblond.ariteg.multihash.MultihashProvider
import info.skyblond.ariteg.multihash.MultihashProviders.mustMatch
import info.skyblond.ariteg.objects.toMultihash
import io.ipfs.multihash.Multihash
import org.slf4j.LoggerFactory
import java.io.File
import java.util.concurrent.CompletableFuture
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.function.BiFunction

// this class use ProtoStatus, which is a sealed class in Kotlin
// Currently not supported by Java, have to implement all
// ProtoStorage in Kotlin
class ProtoFileStorage(
    private val primaryProvider: MultihashProvider,
    private val secondaryProvider: MultihashProvider,
    private val baseDir: File,
    threadNum: Int,
    queueSize: Int
) : ProtoStorageService, AutoCloseable {
    private val logger = LoggerFactory.getLogger(ProtoFileStorage::class.java)

    private val threadPool: ThreadPoolExecutor = ThreadPoolExecutor(
        threadNum, threadNum, 0L, TimeUnit.MILLISECONDS,
        LinkedBlockingQueue(queueSize), ThreadPoolExecutor.CallerRunsPolicy()
    )

    init {
        logger.info("Using base dir: {}", baseDir.canonicalPath)
    }

    private fun multihashToFileMapper(
        base: File, type: ObjectType, primaryHash: Multihash
    ): File {
        val typeDir = File(base, type.name.lowercase())
        if (typeDir.mkdirs()) {
            logger.trace("Create dir: " + typeDir.absolutePath)
        }
        return File(typeDir, primaryHash.toBase58())
    }

    @Throws(StoreProtoException::class)
    override fun storeProto(
        name: String,
        proto: AritegObject,
        checkBeforeWrite: BiFunction<Multihash, Multihash, Boolean>
    ): StoreReceipt {
        // get raw bytes
        val rawBytes = proto.toByteArray()
        // calculate multihash
        val primaryMultihash = primaryProvider.digest(rawBytes)

        val future = CompletableFuture.supplyAsync({
            // calculate secondary hash
            val secondaryMultihash = secondaryProvider.digest(rawBytes)
            // run the check, return if we get false
            if (checkBeforeWrite.apply(primaryMultihash, secondaryMultihash)) {
                // check pass, add request into queue
                val file = multihashToFileMapper(baseDir, proto.type, primaryMultihash)
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
                .setType(proto.type)
                .build(),
            future
        )
    }

    override fun headLink(link: AritegLink): ProtoStatus {
        val file = multihashToFileMapper(
            baseDir, link.type, link.multihash.toMultihash()
        )
        return if (file.exists()) {
            assert(file.length() <= Int.MAX_VALUE) { "File size overflow: ${file.length()}" }
            ProtoStatus.Ready(Long.MAX_VALUE, file.length().toInt())
        } else {
            ProtoStatus.NotFound(getUnixTimestamp())
        }
    }

    override fun restoreLink(link: AritegLink, option: RestoreOption?): CompletableFuture<Void>? {
        return CompletableFuture.runAsync { CompletableFutureUtils.nop() }
    }

    override fun loadProto(link: AritegLink): AritegObject {
        val multihash = link.multihash.toMultihash()
        logger.info("Loading {}", multihash.toBase58())
        val file = multihashToFileMapper(baseDir, link.type, multihash)
        if (!file.exists())
            throw IllegalObjectStatusException(
                "load", link, ProtoStatus.NotFound(getUnixTimestamp())
            )
        file.inputStream().use {
            multihash mustMatch it
        }
        return file.inputStream().use {
            AritegObject.parseFrom(it)
        }
    }

    @Throws(IllegalStateException::class)
    override fun deleteProto(link: AritegLink): Boolean {
        headLink(link).let {
            if (it !is ProtoStatus.Ready) {
                throw IllegalObjectStatusException("delete", link, it)
            }
        }

        return multihashToFileMapper(
            baseDir, link.type, link.multihash.toMultihash()
        ).delete()
    }

    override fun getPrimaryMultihashType(): Multihash.Type = primaryProvider.getType()

    override fun getSecondaryMultihashType(): Multihash.Type = secondaryProvider.getType()

    override fun close() {
        threadPool.shutdown()
        while (!threadPool.awaitTermination(1, TimeUnit.MINUTES)) {
            logger.trace("Waiting termination...")
        }
    }
}
