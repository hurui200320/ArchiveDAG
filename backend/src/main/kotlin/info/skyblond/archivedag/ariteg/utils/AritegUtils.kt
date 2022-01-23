package info.skyblond.archivedag.ariteg.utils

import com.google.protobuf.ByteString
import info.skyblond.archivedag.ariteg.protos.AritegLink
import io.ipfs.multihash.Multihash

fun ByteString.toMultihash(): Multihash {
    return Multihash.deserialize(this.toByteArray())
}

fun AritegLink.toMultihashBase58(): String {
    return this.multihash.toMultihash().toBase58()
}
