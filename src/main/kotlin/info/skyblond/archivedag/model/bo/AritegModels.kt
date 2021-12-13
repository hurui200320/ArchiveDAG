package info.skyblond.archivedag.model.bo

import info.skyblond.ariteg.model.StorageStatus
import info.skyblond.ariteg.protos.AritegLink
import info.skyblond.ariteg.protos.AritegObjectType
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
