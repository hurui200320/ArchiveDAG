package info.skyblond.archivedag.ariteg.storage

import info.skyblond.archivedag.ariteg.model.*
import info.skyblond.archivedag.ariteg.multihash.MultihashProviders
import info.skyblond.archivedag.ariteg.protos.*
import info.skyblond.archivedag.ariteg.utils.toMultihash
import info.skyblond.archivedag.ariteg.utils.toMultihashBase58
import io.ipfs.multihash.Multihash
import org.slf4j.LoggerFactory
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.*
import software.amazon.awssdk.utils.Md5Utils
import java.io.File
import java.time.ZonedDateTime
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.function.BiFunction

class AritegS3ArchiveStorageService(
    primaryProviderType: Multihash.Type,
    secondaryProviderType: Multihash.Type,
    baseDir: File,
    private val s3Client: S3Client,
    private val bucketName: String,
    private val uploadStorageClass: StorageClass,
    threadNum: Int,
    queueSize: Int
) : AritegFileAbstractStorage(primaryProviderType, secondaryProviderType, baseDir), AutoCloseable {
    private val logger = LoggerFactory.getLogger(AritegS3ArchiveStorageService::class.java)

    private val threadPool: ThreadPoolExecutor = ThreadPoolExecutor(
        threadNum, threadNum, 0L, TimeUnit.MILLISECONDS,
        LinkedBlockingQueue(queueSize), ThreadPoolExecutor.CallerRunsPolicy()
    )

    init {
        // will throw exception if bucket not exists
        s3Client.headBucket { it.bucket(bucketName) }
        logger.info("Using bucket: {}", bucketName)
    }

    private fun multihashToKeyMapper(
        type: AritegObjectType, primaryHash: Multihash
    ): String {
        return "${type.name.lowercase()}/${primaryHash.toBase58()}"
    }

    private fun uploadProto(
        primaryMultihash: Multihash, type: AritegObjectType, rawBytes: ByteArray
    ) {
        val key = multihashToKeyMapper(type, primaryMultihash)
        logger.debug("Writing into key `{}`", key)
        s3Client.putObject({
            it.bucket(bucketName)
                .key(key)
                .contentMD5(Md5Utils.md5AsBase64(rawBytes))
                .storageClass(uploadStorageClass)
        }, RequestBody.fromBytes(rawBytes))
    }

    override fun doWrite(primaryMultihash: Multihash, type: AritegObjectType, rawBytes: ByteArray) {
        writeToFile(primaryMultihash, type, rawBytes)
        uploadProto(primaryMultihash, type, rawBytes)
    }

    override fun store(
        name: String,
        proto: AritegObject,
        checkBeforeWrite: BiFunction<Multihash, Multihash, Boolean>
    ): StoreReceipt {
        return storeInternal(
            name, proto.toProto().toByteArray(), proto.getObjectType(),
            checkBeforeWrite, threadPool
        )
    }

    /**
     * Resolve the availability from [HeadObjectResponse],
     * return <available, pending, expiredTimestamp> for [StorageStatus]
     * */
    private fun resolveAvailability(header: HeadObjectResponse): Boolean {
        if (header.storageClass() in listOf(StorageClass.DEEP_ARCHIVE, StorageClass.GLACIER)
            || (header.storageClass() == StorageClass.INTELLIGENT_TIERING && header.archiveStatus() != null)
        ) {
            // is archived
            val restore = header.restore() ?: ""
            val expiration = if (restore.contains("expiry-date=\"")) {
                val expiryDateString = restore.split("expiry-date=\"")[1].split("\"")[0]
                ZonedDateTime.parse(expiryDateString).toInstant().epochSecond
            } else {
                null
            }
            // no expiration -> not available
            // has expiration -> available now
            return expiration != null
        } else {
            // normal object -> always available
            return true
        }
    }

    override fun queryStatus(link: AritegLink): StorageStatus? {
        val fileResult = queryFile(link)
        if (fileResult != null)
            return fileResult
        // file result is null, query s3
        val key = multihashToKeyMapper(link.type, link.multihash.toMultihash())
        val headResp = try {
            s3Client.headObject { it.bucket(bucketName).key(key) }
        } catch (_: NoSuchKeyException) {
            return null  // really not found
        }
        logger.warn("link {} not found in local but exists in S3", link.multihash.toMultihash().toBase58())
        // found in S3 but not in local -> local corrupted
        // return not available but can query size from S3
        // set size to null means not in the local file
        return StorageStatus(resolveAvailability(headResp), null)
    }

    override fun restoreLink(link: AritegLink) {
        try {
            s3Client.restoreObject { b ->
                b.bucket(bucketName)
                    .key(multihashToKeyMapper(link.type, link.multihash.toMultihash()))
                    .restoreRequest {
                        it.days(3).tier(Tier.BULK)
                    }
            }
        } catch (e: InvalidObjectStateException) {
            throw IllegalStateException(e) // not suitable for restore
        }
    }

    override fun loadProto(link: AritegLink): AritegObject {
        val fileResult = loadFromFile(link)
        if (fileResult != null)
            return fileResult
        // file not found, try S3
        val multihash = link.multihash.toMultihash()
        val key = multihashToKeyMapper(link.type, multihash)
        val content = try {
            s3Client.getObject { it.bucket(bucketName).key(key) }.readAllBytes()
        } catch (_: NoSuchKeyException) {
            throw IllegalStateException("Cannot load ${link.toMultihashBase58()}: not found")
        } catch (_: InvalidObjectStateException) {
            throw IllegalStateException("Cannot load ${link.toMultihashBase58()}: unavailable")
        }
        // validate multihash
        MultihashProviders.mustMatch(multihash, content)
        // write to local
        writeToFile(multihash, link.type, content)
        return when (link.type) {
            AritegObjectType.BLOB -> BlobObject.fromProto(AritegBlobObject.parseFrom(content))
            AritegObjectType.LIST -> ListObject.fromProto(AritegListObject.parseFrom(content))
            AritegObjectType.TREE -> TreeObject.fromProto(AritegTreeObject.parseFrom(content))
            AritegObjectType.COMMIT -> CommitObject.fromProto(AritegCommitObject.parseFrom(content))
            else -> throw IllegalStateException("Invalid object type: ${link.type}")
        }
    }

    override fun deleteProto(link: AritegLink): Boolean {
        val fileResult = deleteFile(link)
        val s3Result = try {
            s3Client.deleteObject {
                it.bucket(bucketName)
                    .key(multihashToKeyMapper(link.type, link.multihash.toMultihash()))
            }
            true
        } catch (_: Throwable) {
            false
        }

        return fileResult && s3Result
    }

    override fun close() {
        threadPool.shutdown()
        while (!threadPool.awaitTermination(1, TimeUnit.MINUTES)) {
            logger.trace("Waiting termination...")
        }
    }
}
