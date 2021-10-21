package info.skyblond.ariteg.objects

import com.google.protobuf.ByteString
import info.skyblond.ariteg.AritegLink
import io.ipfs.multihash.Multihash

fun ByteString.toMultihash(): Multihash {
    return Multihash.deserialize(this.toByteArray())
}

fun AritegLink.toMultihashBase58(): String {
    return this.multihash.toMultihash().toBase58()
}
