package info.skyblond.archivedag.model.bo

import info.skyblond.ariteg.AritegLink
import info.skyblond.ariteg.ObjectType
import io.ipfs.multihash.Multihash
import java.io.InputStream
import java.util.concurrent.CompletableFuture

/**
 * Enum representing the status of an object.
 * */
sealed class ProtoStatus(
    val name: String,
    val objSize: Int,
) {
    /**
     * The object is ready to read. No extra operation is needed.
     * The object is not guaranteed to be ready after [expiredAt].
     * [expiredAt] is the timestamp in second
     * */
    class Ready(
        val expiredAt: Long,
        _objSize: Int,
    ) : ProtoStatus("ready", _objSize)

    /**
     * The object is archived, need to restore before read.
     * The object is archived at the [queriedAt] timestamp.
     * */
    class Archived(
        val queriedAt: Long,
        _objSize: Int,
    ) : ProtoStatus("archived", _objSize)

    /**
     * The object is restoring.
     * The restoration started at [startedAt] timestamp,
     * and is expected to finish at [expectFinishedTime].
     * */
    class Restoring(
        val startedAt: Long,
        val expectFinishedTime: Long,
        _objSize: Int,
    ) : ProtoStatus("restoring", _objSize)

    /**
     * The object is not present.
     * It might be writing, or just don't exist.
     * */
    class NotFound(
        val queriedAt: Long
    ) : ProtoStatus("not found", -1)
}

/**
 * Receipt for store operation.
 * Replace [Pair] <AritegLink, CompletableFuture<*>>
 * */
data class StoreReceipt(
    val link: AritegLink,
    val completionFuture: CompletableFuture<Multihash?>
)

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
    val status: ProtoStatus,
    // TODO more data needed?
)

/**
 * Interface of restore option.
 * Different storage client may require different option.
 * */
data class RestoreOption(
    val days: Int
)

data class FindTypeReceipt(
    val objectType: ObjectType,
    val mediaType: String?
)
