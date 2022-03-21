package info.skyblond.archivedag.ariteg.multihash

import io.ipfs.multihash.Multihash
import java.io.InputStream
import java.security.MessageDigest

/**
 * This provider will use [java.security.MessageDigest] to calculate
 * hash, and return them into the format of [io.ipfs.multihash]
 * */
class MultihashJavaProvider(
    private val hashType: Multihash.Type,
    private val provider: () -> MessageDigest
) : MultihashProvider {
    override fun getType(): Multihash.Type {
        return hashType
    }

    override fun digest(byteArray: ByteArray): Multihash {
        // get a new message digest every time
        val digest = provider()
        val output = digest.digest(byteArray)
        return Multihash(hashType, output)
    }

    override fun digest(inputStream: InputStream, bufferSize: Int): Multihash {
        val digest = provider()
        val buffer = ByteArray(bufferSize)
        var bytesReadCount: Int
        while (inputStream.read(buffer, 0, buffer.size)
                .also { bytesReadCount = it } != -1
        ) {
            digest.update(buffer, 0, bytesReadCount)
        }
        return Multihash(hashType, digest.digest())
    }
}
