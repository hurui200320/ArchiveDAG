package info.skyblond.archivedag.ariteg.multihash

import io.ipfs.multihash.Multihash
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

internal class MultihashProvidersTest {
    // Type -> Empty hash (base58)
    private val emptySamples = mapOf(
        Multihash.Type.sha3_256 to "W1kknXZLRvyN91meETWtiTKmiAYM4HNtyHekcEPZXYB8Tj",
        Multihash.Type.sha3_512 to "8tXPg8VsVLVrv2sSepoKfnbqoFa94UQ3bQw21qgyajHFgbVBRtKLvTjHA33fiRY29RGK1KoEff776Ly8UrUh4dBTnV",
        Multihash.Type.blake2b_256 to "2Drjgb5DseoVAvRLngcVmd4YfJAi3J1145kiNFV3CL32Hs6vzb",
        Multihash.Type.blake2b_384 to "jThd1Fu19VvzvRfENEm2JGsvkc9yauns2Gozrjq3SVxDCRQ5GedeqNACfL6PdmVissQjQaB",
        Multihash.Type.blake2b_512 to "SEfXUCRqQ9o9q17v3B1UoSXmRVZMsUVCrjkc4SR6h2Btpzbqe27J55bxSvpb3hCHDKCekJXB12sJAQcD1RT1nXcEW3ejP",
        Multihash.Type.blake2s_128 to "3unuYPZ5Km2ZQdPNE5Xp6eckqzUT",
        Multihash.Type.blake2s_256 to "2i3XjxBo8oapLVB31oescuSKGELZnP2HVqaLywnTcCy1FCiSCY",
    )

    @Test
    fun testDigestEmptyArray() {
        val emptyByteArray = ByteArray(0)
        emptySamples.forEach { (type, expectBase58) ->
            val provider = MultihashProviders.fromMultihashType(type)
            assertEquals(type, provider.getType())
            val result = provider.digest(emptyByteArray)
            assertEquals(expectBase58, result.toBase58())
        }
    }

    @Test
    fun testDigestEmptyStream() {
        val emptyByteArray = ByteArray(0)
        emptySamples.forEach { (type, expectBase58) ->
            val provider = MultihashProviders.fromMultihashType(type)
            val result = emptyByteArray.inputStream().use { provider.digest(it) }
            assertEquals(expectBase58, result.toBase58())
        }
    }

    @Test
    fun testMustMatchEmptyArray() {
        val emptyByteArray = ByteArray(0)
        val unexpectedArray = "Unexpected".encodeToByteArray()
        emptySamples.forEach { (_, base58) ->
            val expectedMultihash = Multihash.fromBase58(base58)
            assertDoesNotThrow {
                MultihashProviders.mustMatch(
                    expectedMultihash, emptyByteArray
                )
            }
            assertThrows<IllegalStateException> {
                MultihashProviders.mustMatch(
                    expectedMultihash, unexpectedArray
                )
            }
        }
    }

    @Test
    fun testMustMatchEmptyStream() {
        val emptyByteArray = ByteArray(0)
        val unexpectedArray = "Unexpected".encodeToByteArray()
        emptySamples.forEach { (_, base58) ->
            val expectedMultihash = Multihash.fromBase58(base58)
            assertDoesNotThrow {
                emptyByteArray.inputStream().use {
                    MultihashProviders.mustMatch(
                        expectedMultihash, it
                    )
                }
            }
            assertThrows<IllegalStateException> {
                unexpectedArray.inputStream().use {
                    MultihashProviders.mustMatch(
                        expectedMultihash, it
                    )
                }
            }
        }
    }

    @Test
    fun testNotImplemented() {
        assertThrows<NotImplementedError> {
            // this is an unsafe hash, shouldn't be implemented
            MultihashProviders.fromMultihashType(Multihash.Type.sha1)
        }
    }
}
