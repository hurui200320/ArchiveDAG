package info.skyblond.ariteg.multihash

import io.ipfs.multihash.Multihash
import org.bouncycastle.crypto.Digest
import java.io.InputStream

/**
 * This provider will use [java.security.MessageDigest] to calculate
 * hash, and return them into the format of [io.ipfs.multihash]
 * */
class MultihashBouncyCastleProvider(
    private val hashType: Multihash.Type,
    private val provider: () -> Digest
) : MultihashProvider {
    override fun getType(): Multihash.Type {
        return hashType
    }

    override fun digest(byteArray: ByteArray): Multihash {
        // get a new message digest every time
        val digest = provider()
        val output = ByteArray(digest.digestSize)
        digest.update(byteArray, 0, byteArray.size)
        digest.doFinal(output, 0)
        return Multihash(hashType, output)
    }

    override fun digest(inputStream: InputStream, bufferSize: Int): Multihash {
        val digest = provider()
        val output = ByteArray(digest.digestSize)
        val buffer = ByteArray(bufferSize)
        var bytesReadCount: Int
        while (inputStream.read(buffer, 0, buffer.size)
                .also { bytesReadCount = it } != -1
        ) {
            digest.update(buffer, 0, bytesReadCount)
        }
        digest.doFinal(output, 0)
        return Multihash(hashType, output)
    }
}
