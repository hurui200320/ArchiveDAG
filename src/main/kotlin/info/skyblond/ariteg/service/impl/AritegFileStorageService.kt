package info.skyblond.ariteg.service.impl

import com.google.protobuf.ByteString
import info.skyblond.archivedag.model.IllegalObjectStatusException
import info.skyblond.archivedag.model.LoadProtoException
import info.skyblond.archivedag.util.getUnixTimestamp
import info.skyblond.ariteg.model.*
import info.skyblond.ariteg.multihash.MultihashProvider
import info.skyblond.ariteg.multihash.MultihashProviders.mustMatch
import info.skyblond.ariteg.protos.*
import info.skyblond.ariteg.service.intf.AritegStorageService
import info.skyblond.ariteg.util.CompletableFutureUtils
import info.skyblond.ariteg.util.toMultihash
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
        blob: BlobObject,
        checkBeforeWrite: BiFunction<Multihash, Multihash, Boolean>
    ): StoreReceipt {
        return storeInternal(
            name, blob.toProto().toByteArray(),
            AritegObjectType.BLOB, checkBeforeWrite
        )
    }

    override fun store(
        name: String,
        list: ListObject,
        checkBeforeWrite: BiFunction<Multihash, Multihash, Boolean>
    ): StoreReceipt {
        return storeInternal(
            name, list.toProto().toByteArray(),
            AritegObjectType.LIST, checkBeforeWrite
        )
    }

    override fun store(
        name: String,
        tree: TreeObject,
        checkBeforeWrite: BiFunction<Multihash, Multihash, Boolean>
    ): StoreReceipt {
        return storeInternal(
            name, tree.toProto().toByteArray(),
            AritegObjectType.TREE, checkBeforeWrite
        )
    }

    override fun store(
        name: String,
        commitObject: CommitObject,
        checkBeforeWrite: BiFunction<Multihash, Multihash, Boolean>
    ): StoreReceipt {
        return storeInternal(
            name, commitObject.toProto().toByteArray(),
            AritegObjectType.COMMIT, checkBeforeWrite
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

    override fun restoreLink(link: AritegLink, option: RestoreOption?): CompletableFuture<Void> {
        // no need to restore on disk
        return CompletableFuture.runAsync { CompletableFutureUtils.nop() }
    }

    override fun loadProto(link: AritegLink): Pair<AritegObjectType, Any?> {
        val multihash = link.multihash.toMultihash()
        logger.info("Loading {}", multihash.toBase58())
        val file = multihashToFileMapper(baseDir, link.type, multihash)
        if (!file.exists())
            throw IllegalObjectStatusException("load", link, "not found")
        file.inputStream().use {
            multihash mustMatch it
        }
        when (link.type!!) {
            AritegObjectType.NULL -> throw LoadProtoException(Throwable("Invalid object type: NULL"))
            AritegObjectType.BLOB -> return AritegObjectType.BLOB to file.inputStream().use {
                BlobObject.fromProto(AritegBlobObject.parseFrom(it))
            }
            AritegObjectType.LIST -> return AritegObjectType.LIST to file.inputStream().use {
                ListObject.fromProto(AritegListObject.parseFrom(it))
            }
            AritegObjectType.TREE -> return AritegObjectType.TREE to file.inputStream().use {
                TreeObject.fromProto(AritegTreeObject.parseFrom(it))
            }
            AritegObjectType.COMMIT -> return AritegObjectType.COMMIT to file.inputStream().use {
                CommitObject.fromProto(AritegCommitObject.parseFrom(it))
            }
            AritegObjectType.UNRECOGNIZED -> throw LoadProtoException(Throwable("Invalid object type: UNRECOGNIZED"))
        }
    }

    override fun deleteProto(link: AritegLink): Boolean {
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
