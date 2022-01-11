package info.skyblond.archivedag.ariteg.storage

import com.google.protobuf.ByteString
import info.skyblond.archivedag.ariteg.model.*
import info.skyblond.archivedag.ariteg.multihash.MultihashProviders
import info.skyblond.archivedag.ariteg.protos.*
import info.skyblond.archivedag.ariteg.utils.toMultihash
import io.ipfs.multihash.Multihash
import org.slf4j.LoggerFactory
import java.io.File
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.function.BiFunction

abstract class AritegFileAbstractStorage(
    primaryProviderType: Multihash.Type,
    secondaryProviderType: Multihash.Type,
    private val baseDir: File,
) : AritegStorageService {
    private val logger = LoggerFactory.getLogger(AritegFileAbstractStorage::class.java)
    private val primaryProvider = MultihashProviders.fromMultihashType(primaryProviderType)
    private val secondaryProvider = MultihashProviders.fromMultihashType(secondaryProviderType)

    init {
        logger.info("Using base dir: {}", baseDir.canonicalPath)
    }

    /**
     * Map a <multihash, type> to a local file
     * */
    private fun multihashToFileMapper(
        type: AritegObjectType, primaryHash: Multihash
    ): File {
        val typeDir = File(baseDir, type.name.lowercase())
        if (typeDir.mkdirs()) {
            logger.trace("Create dir: " + typeDir.absolutePath)
        }
        return File(typeDir, primaryHash.toBase58())
    }

    abstract fun doWrite(primaryMultihash: Multihash, type: AritegObjectType, rawBytes: ByteArray)

    protected fun writeToFile(primaryMultihash: Multihash, type: AritegObjectType, rawBytes: ByteArray) {
        val file = multihashToFileMapper(type, primaryMultihash)
        logger.debug("Writing into file `{}`", file.canonicalPath)
        file.writeBytes(rawBytes)
    }

    protected fun storeInternal(
        name: String,
        rawBytes: ByteArray,
        type: AritegObjectType,
        checkBeforeWrite: BiFunction<Multihash, Multihash, Boolean>,
        threadPool: Executor
    ): StoreReceipt {
        require(rawBytes.size <= 16 * 1024 * 1024) { "Hard limit reached: 16MB" }
        // calculate multihash
        val primaryMultihash = primaryProvider.digest(rawBytes)
        val future = CompletableFuture.supplyAsync({
            // calculate secondary hash
            val secondaryMultihash = secondaryProvider.digest(rawBytes)
            // run the check, return if we get false
            if (checkBeforeWrite.apply(primaryMultihash, secondaryMultihash)) {
                // check pass, add request into queue
                doWrite(primaryMultihash, type, rawBytes)
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

    protected fun queryFile(link: AritegLink): StorageStatus? {
        val file = multihashToFileMapper(
            link.type, link.multihash.toMultihash()
        )
        return if (file.exists()) {
            StorageStatus(
                available = true,
                protoSize = file.length()
            )
        } else {
            null
        }
    }

    protected fun loadFromFile(link: AritegLink): AritegObject? {
        val multihash = link.multihash.toMultihash()
        logger.info("Loading {}", multihash.toBase58())
        val file = multihashToFileMapper(link.type, multihash)
        if (!file.exists())
            return null
        file.inputStream().use {
            MultihashProviders.mustMatch(multihash, it)
        }
        return file.inputStream().use {
            when (link.type) {
                AritegObjectType.BLOB -> BlobObject.fromProto(AritegBlobObject.parseFrom(it))
                AritegObjectType.LIST -> ListObject.fromProto(AritegListObject.parseFrom(it))
                AritegObjectType.TREE -> TreeObject.fromProto(AritegTreeObject.parseFrom(it))
                AritegObjectType.COMMIT -> CommitObject.fromProto(AritegCommitObject.parseFrom(it))
                else -> throw IllegalStateException("Invalid object type: ${link.type}")
            }
        }
    }

    protected fun deleteFile(link: AritegLink): Boolean {
        return multihashToFileMapper(link.type, link.multihash.toMultihash()).delete()
    }
}
