package info.skyblond.ariteg.multihash

import info.skyblond.archivedag.model.exception.MultihashNotMatchError
import io.ipfs.multihash.Multihash
import io.ipfs.multihash.Multihash.Type
import org.bouncycastle.crypto.digests.Blake2bDigest
import org.bouncycastle.crypto.digests.Blake2sDigest
import org.bouncycastle.crypto.digests.KeccakDigest
import org.bouncycastle.crypto.digests.SHA512Digest
import java.io.InputStream
import java.security.MessageDigest

object MultihashProviders {

    infix fun Multihash.mustMatch(byteArray: ByteArray) {
        val provider = fromMultihashType(this.type)
        val target = provider.digest(byteArray)
        if (this != target)
            throw MultihashNotMatchError(this, target)
    }

    infix fun Multihash.mustMatch(inputStream: InputStream) {
        val provider = fromMultihashType(this.type)
        val target = provider.digest(inputStream)
        if (this != target)
            throw MultihashNotMatchError(this, target)
    }

    private fun generateJavaProvider(type: Type, algorithm: String): MultihashJavaProvider {
        return MultihashJavaProvider(type) {
            MessageDigest.getInstance(algorithm)
        }
    }

    private fun generateKeccakProvider(type: Type, bitLength: Int): MultihashBouncyCastleProvider {
        return MultihashBouncyCastleProvider(type) {
            KeccakDigest(bitLength)
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
    fun fromMultihashType(type: Type): MultihashProvider = when (type) {
        Type.id -> object : MultihashProvider {
            override fun getType(): Type = Type.id

            override fun digest(byteArray: ByteArray): Multihash =
                Multihash(Type.id, byteArray)

            override fun digest(inputStream: InputStream, bufferSize: Int): Multihash =
                Multihash(Type.id, inputStream.readAllBytes())
        }
        Type.md5 -> generateJavaProvider(Type.md5, "MD5")
        Type.sha1 -> generateJavaProvider(Type.sha1, "SHA-1")
        Type.sha2_256 -> generateJavaProvider(Type.sha2_256, "SHA-256")
        Type.sha3_256 -> generateJavaProvider(Type.sha3_256, "SHA3-256")
        Type.sha3_512 -> generateJavaProvider(Type.sha3_512, "SHA3-512")
        Type.sha2_512 -> MultihashBouncyCastleProvider(Type.sha2_512) { SHA512Digest() }
        Type.keccak_256 -> generateKeccakProvider(Type.keccak_256, 256)
        Type.keccak_384 -> generateKeccakProvider(Type.keccak_384, 384)
        Type.keccak_512 -> generateKeccakProvider(Type.keccak_512, 512)
        Type.blake2b_8 -> generateBlake2bProvider(Type.blake2b_8, 8)
        Type.blake2b_16 -> generateBlake2bProvider(Type.blake2b_16, 16)
        Type.blake2b_24 -> generateBlake2bProvider(Type.blake2b_24, 24)
        Type.blake2b_32 -> generateBlake2bProvider(Type.blake2b_32, 32)
        Type.blake2b_40 -> generateBlake2bProvider(Type.blake2b_40, 40)
        Type.blake2b_48 -> generateBlake2bProvider(Type.blake2b_48, 48)
        Type.blake2b_56 -> generateBlake2bProvider(Type.blake2b_56, 56)
        Type.blake2b_64 -> generateBlake2bProvider(Type.blake2b_64, 64)
        Type.blake2b_72 -> generateBlake2bProvider(Type.blake2b_72, 72)
        Type.blake2b_80 -> generateBlake2bProvider(Type.blake2b_80, 80)
        Type.blake2b_88 -> generateBlake2bProvider(Type.blake2b_88, 88)
        Type.blake2b_96 -> generateBlake2bProvider(Type.blake2b_96, 96)
        Type.blake2b_104 -> generateBlake2bProvider(Type.blake2b_104, 104)
        Type.blake2b_112 -> generateBlake2bProvider(Type.blake2b_112, 112)
        Type.blake2b_120 -> generateBlake2bProvider(Type.blake2b_120, 120)
        Type.blake2b_128 -> generateBlake2bProvider(Type.blake2b_128, 128)
        Type.blake2b_136 -> generateBlake2bProvider(Type.blake2b_136, 136)
        Type.blake2b_144 -> generateBlake2bProvider(Type.blake2b_144, 144)
        Type.blake2b_152 -> generateBlake2bProvider(Type.blake2b_152, 152)
        Type.blake2b_160 -> generateBlake2bProvider(Type.blake2b_160, 160)
        Type.blake2b_168 -> generateBlake2bProvider(Type.blake2b_168, 168)
        Type.blake2b_176 -> generateBlake2bProvider(Type.blake2b_176, 176)
        Type.blake2b_184 -> generateBlake2bProvider(Type.blake2b_184, 184)
        Type.blake2b_192 -> generateBlake2bProvider(Type.blake2b_192, 192)
        Type.blake2b_200 -> generateBlake2bProvider(Type.blake2b_200, 200)
        Type.blake2b_208 -> generateBlake2bProvider(Type.blake2b_208, 208)
        Type.blake2b_216 -> generateBlake2bProvider(Type.blake2b_216, 216)
        Type.blake2b_224 -> generateBlake2bProvider(Type.blake2b_224, 224)
        Type.blake2b_232 -> generateBlake2bProvider(Type.blake2b_232, 232)
        Type.blake2b_240 -> generateBlake2bProvider(Type.blake2b_240, 240)
        Type.blake2b_248 -> generateBlake2bProvider(Type.blake2b_248, 248)
        Type.blake2b_256 -> generateBlake2bProvider(Type.blake2b_256, 256)
        Type.blake2b_264 -> generateBlake2bProvider(Type.blake2b_264, 264)
        Type.blake2b_272 -> generateBlake2bProvider(Type.blake2b_272, 272)
        Type.blake2b_280 -> generateBlake2bProvider(Type.blake2b_280, 280)
        Type.blake2b_288 -> generateBlake2bProvider(Type.blake2b_288, 288)
        Type.blake2b_296 -> generateBlake2bProvider(Type.blake2b_296, 296)
        Type.blake2b_304 -> generateBlake2bProvider(Type.blake2b_304, 304)
        Type.blake2b_312 -> generateBlake2bProvider(Type.blake2b_312, 312)
        Type.blake2b_320 -> generateBlake2bProvider(Type.blake2b_320, 320)
        Type.blake2b_328 -> generateBlake2bProvider(Type.blake2b_328, 328)
        Type.blake2b_336 -> generateBlake2bProvider(Type.blake2b_336, 336)
        Type.blake2b_344 -> generateBlake2bProvider(Type.blake2b_344, 344)
        Type.blake2b_352 -> generateBlake2bProvider(Type.blake2b_352, 352)
        Type.blake2b_360 -> generateBlake2bProvider(Type.blake2b_360, 360)
        Type.blake2b_368 -> generateBlake2bProvider(Type.blake2b_368, 368)
        Type.blake2b_376 -> generateBlake2bProvider(Type.blake2b_376, 376)
        Type.blake2b_384 -> generateBlake2bProvider(Type.blake2b_384, 384)
        Type.blake2b_392 -> generateBlake2bProvider(Type.blake2b_392, 392)
        Type.blake2b_400 -> generateBlake2bProvider(Type.blake2b_400, 400)
        Type.blake2b_408 -> generateBlake2bProvider(Type.blake2b_408, 408)
        Type.blake2b_416 -> generateBlake2bProvider(Type.blake2b_416, 416)
        Type.blake2b_424 -> generateBlake2bProvider(Type.blake2b_424, 424)
        Type.blake2b_432 -> generateBlake2bProvider(Type.blake2b_432, 432)
        Type.blake2b_440 -> generateBlake2bProvider(Type.blake2b_440, 440)
        Type.blake2b_448 -> generateBlake2bProvider(Type.blake2b_448, 448)
        Type.blake2b_456 -> generateBlake2bProvider(Type.blake2b_456, 456)
        Type.blake2b_464 -> generateBlake2bProvider(Type.blake2b_464, 464)
        Type.blake2b_472 -> generateBlake2bProvider(Type.blake2b_472, 472)
        Type.blake2b_480 -> generateBlake2bProvider(Type.blake2b_480, 480)
        Type.blake2b_488 -> generateBlake2bProvider(Type.blake2b_488, 488)
        Type.blake2b_496 -> generateBlake2bProvider(Type.blake2b_496, 496)
        Type.blake2b_504 -> generateBlake2bProvider(Type.blake2b_504, 504)
        Type.blake2b_512 -> generateBlake2bProvider(Type.blake2b_512, 512)
        Type.blake2s_8 -> generateBlake2sProvider(Type.blake2s_8, 8)
        Type.blake2s_16 -> generateBlake2sProvider(Type.blake2s_16, 16)
        Type.blake2s_24 -> generateBlake2sProvider(Type.blake2s_24, 24)
        Type.blake2s_32 -> generateBlake2sProvider(Type.blake2s_32, 32)
        Type.blake2s_40 -> generateBlake2sProvider(Type.blake2s_40, 40)
        Type.blake2s_48 -> generateBlake2sProvider(Type.blake2s_48, 48)
        Type.blake2s_56 -> generateBlake2sProvider(Type.blake2s_56, 56)
        Type.blake2s_64 -> generateBlake2sProvider(Type.blake2s_64, 64)
        Type.blake2s_72 -> generateBlake2sProvider(Type.blake2s_72, 72)
        Type.blake2s_80 -> generateBlake2sProvider(Type.blake2s_80, 80)
        Type.blake2s_88 -> generateBlake2sProvider(Type.blake2s_88, 88)
        Type.blake2s_96 -> generateBlake2sProvider(Type.blake2s_96, 96)
        Type.blake2s_104 -> generateBlake2sProvider(Type.blake2s_104, 104)
        Type.blake2s_112 -> generateBlake2sProvider(Type.blake2s_112, 112)
        Type.blake2s_120 -> generateBlake2sProvider(Type.blake2s_120, 120)
        Type.blake2s_128 -> generateBlake2sProvider(Type.blake2s_128, 128)
        Type.blake2s_136 -> generateBlake2sProvider(Type.blake2s_136, 136)
        Type.blake2s_144 -> generateBlake2sProvider(Type.blake2s_144, 144)
        Type.blake2s_152 -> generateBlake2sProvider(Type.blake2s_152, 152)
        Type.blake2s_160 -> generateBlake2sProvider(Type.blake2s_160, 160)
        Type.blake2s_168 -> generateBlake2sProvider(Type.blake2s_168, 168)
        Type.blake2s_176 -> generateBlake2sProvider(Type.blake2s_176, 176)
        Type.blake2s_184 -> generateBlake2sProvider(Type.blake2s_184, 184)
        Type.blake2s_192 -> generateBlake2sProvider(Type.blake2s_192, 192)
        Type.blake2s_200 -> generateBlake2sProvider(Type.blake2s_200, 200)
        Type.blake2s_208 -> generateBlake2sProvider(Type.blake2s_208, 208)
        Type.blake2s_216 -> generateBlake2sProvider(Type.blake2s_216, 216)
        Type.blake2s_224 -> generateBlake2sProvider(Type.blake2s_224, 224)
        Type.blake2s_232 -> generateBlake2sProvider(Type.blake2s_232, 232)
        Type.blake2s_240 -> generateBlake2sProvider(Type.blake2s_240, 240)
        Type.blake2s_248 -> generateBlake2sProvider(Type.blake2s_248, 248)
        Type.blake2s_256 -> generateBlake2sProvider(Type.blake2s_256, 256)
        else -> TODO("Not implemented")
    }
}
