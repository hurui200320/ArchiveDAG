package info.skyblond.archivedag.ariteg.model

import info.skyblond.archivedag.ariteg.protos.AritegLink
import io.ipfs.multihash.Multihash
import java.util.concurrent.CompletableFuture

data class FindMetaReceipt(
    val secondaryMultihash: Multihash,
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

@Deprecated("Not used")
data class RestoreReceipt(
    val involvedLinks: List<AritegLink>
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
     * Whether this proto is available to read.
     * */
    val available: Boolean,

    /**
     * The size of this proto file (measured in bytes).
     * null means size not available.
     * */
    val protoSize: Long?
)
