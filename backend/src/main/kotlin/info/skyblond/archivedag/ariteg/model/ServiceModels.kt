package info.skyblond.archivedag.ariteg.model

import info.skyblond.archivedag.ariteg.protos.AritegLink
import info.skyblond.archivedag.ariteg.protos.AritegObjectType
import io.ipfs.multihash.Multihash
import java.io.InputStream
import java.util.concurrent.CompletableFuture

data class FindTypeReceipt(
    val objectType: AritegObjectType,
    val mediaType: String?
)

/**
 * The result of write.
 * [link] is the [AritegLink] point to the root proto.
 * [completionFuture] is the [CompletableFuture] for tracking the writing task.
 * The task is finished if and only if every proto is properly written into the
 * storage.
 * */
data class WriteReceipt(
    val link: AritegLink,
    val completionFuture: CompletableFuture<Void>
)

data class ReadReceipt(
    val mediaType: String,
    val inputStream: InputStream
)

data class RestoreReceipt(
    val involvedLinks: List<AritegLink>,
    val completionFuture: CompletableFuture<Void>
)

data class ProbeReceipt(
    val link: AritegLink,
    val mediaType: String?,
    val status: StorageStatus,
    // TODO more data needed?
)

/**
 * The result of store.
 * [link] is the [AritegLink] point to the proto.
 * [completionFuture] is the [CompletableFuture] for tracking the writing task.
 * The [Multihash] of proto will be returned if the proto has been written,
 * otherwise null is returned.
 * */
data class StoreReceipt(
    val link: AritegLink,
    val completionFuture: CompletableFuture<Multihash?>
)

data class StorageStatus(
    /**
     * When this proto is available to read.
     * -1 means not available yet.
     * */
    val availableFrom: Long,

    /**
     * When this proto is expired and no longer available to read.
     * -1 means won't expire.
     * */
    val expiredTimestamp: Long,

    /**
     * The size of this proto file (measured in bytes).
     * -1 means size not available.
     * */
    val protoSize: Int
)
