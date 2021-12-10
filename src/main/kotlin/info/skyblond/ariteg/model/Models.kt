package info.skyblond.ariteg.model

import info.skyblond.ariteg.protos.AritegLink
import io.ipfs.multihash.Multihash
import java.util.concurrent.CompletableFuture

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

/**
 * Abstract interface for restore protos
 * */
interface RestoreOption
