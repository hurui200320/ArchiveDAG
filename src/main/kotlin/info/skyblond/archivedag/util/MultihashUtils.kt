package info.skyblond.archivedag.util

import info.skyblond.ariteg.AritegLink
import io.ipfs.multihash.Multihash

object MultihashUtils {
    @JvmStatic
    fun getFromAritegLink(link: AritegLink): Multihash {
        return Multihash.deserialize(link.multihash.toByteArray())
    }
}
