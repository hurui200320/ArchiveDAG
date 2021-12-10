package info.skyblond.ariteg.util

import com.google.protobuf.ByteString
import info.skyblond.ariteg.protos.AritegLink
import io.ipfs.multihash.Multihash


fun ByteString.toMultihash(): Multihash {
    return Multihash.deserialize(this.toByteArray())
}

fun AritegLink.toMultihashBase58(): String {
    return this.multihash.toMultihash().toBase58()
}
