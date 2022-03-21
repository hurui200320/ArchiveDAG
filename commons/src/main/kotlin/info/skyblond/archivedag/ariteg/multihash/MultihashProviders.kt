package info.skyblond.archivedag.ariteg.multihash

import io.ipfs.multihash.Multihash
import io.ipfs.multihash.Multihash.Type
import org.bouncycastle.crypto.digests.Blake2bDigest
import org.bouncycastle.crypto.digests.Blake2sDigest
import java.io.InputStream
import java.security.MessageDigest

object MultihashProviders {

    private fun generateJavaProvider(type: Type, algorithm: String): MultihashJavaProvider {
        return MultihashJavaProvider(type) {
            MessageDigest.getInstance(algorithm)
        }
    }

    private fun generateBlake2bProvider(type: Type, digestSize: Int): MultihashBouncyCastleProvider {
        return MultihashBouncyCastleProvider(type) {
            Blake2bDigest(digestSize)
        }
    }

    private fun generateBlake2sProvider(type: Type, digestSize: Int): MultihashBouncyCastleProvider {
        return MultihashBouncyCastleProvider(type) {
            Blake2sDigest(digestSize)
        }
    }

    @JvmStatic
    fun mustMatch(expected: Multihash, byteArray: ByteArray) {
        val provider = fromMultihashType(expected.type)
        val target = provider.digest(byteArray)
        if (expected != target)
            throw IllegalStateException("Multihash not match. Except " + expected.toBase58() + ", but get: " + target.toBase58())
    }

    @JvmStatic
    fun mustMatch(expected: Multihash, inputStream: InputStream) {
        val provider = fromMultihashType(expected.type)
        val target = provider.digest(inputStream)
        if (expected != target)
            throw IllegalStateException("Multihash not match. Except " + expected.toBase58() + ", but get: " + target.toBase58())
    }

    @JvmStatic
    fun fromMultihashType(type: Type): MultihashProvider = when (type) {
        Type.sha3_256 -> generateJavaProvider(Type.sha3_256, "SHA3-256")
        Type.sha3_512 -> generateJavaProvider(Type.sha3_512, "SHA3-512")
        Type.blake2b_256 -> generateBlake2bProvider(Type.blake2b_256, 256)
        Type.blake2b_384 -> generateBlake2bProvider(Type.blake2b_384, 384)
        Type.blake2b_512 -> generateBlake2bProvider(Type.blake2b_512, 512)
        Type.blake2s_128 -> generateBlake2sProvider(Type.blake2s_128, 128)
        Type.blake2s_256 -> generateBlake2sProvider(Type.blake2s_256, 256)
        else -> throw NotImplementedError("Type not implemented")
    }
}
