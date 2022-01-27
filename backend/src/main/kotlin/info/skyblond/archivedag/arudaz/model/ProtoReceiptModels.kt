package info.skyblond.archivedag.arudaz.model

import io.ipfs.multihash.Multihash
import java.util.*

data class ProtoReceipt(
    val recordId: UUID,
    val username: String,
    val primaryHash: Multihash
)
