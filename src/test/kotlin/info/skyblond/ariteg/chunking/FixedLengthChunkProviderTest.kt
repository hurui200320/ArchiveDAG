package info.skyblond.ariteg.chunking

import com.google.protobuf.ByteString
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.random.Random

internal class FixedLengthChunkProviderTest {

    @Test
    fun testSmallChunk() {
        val bytes = ByteArray(3)
        Random.nextBytes(bytes)
        val expected = ByteString.copyFrom(bytes)
        val chunkProvider = FixedLengthChunkProvider(bytes.inputStream(), 16)
        assertEquals(expected, chunkProvider.nextChunk())
        assertEquals(ByteString.EMPTY, chunkProvider.nextChunk())
        chunkProvider.close()
    }

    @Test
    fun testNormalChunk() {
        val bytes = ByteArray(18)
        Random.nextBytes(bytes)
        val expected1 = ByteString.copyFrom(bytes, 0, 16)
        val expected2 = ByteString.copyFrom(bytes, 16, 2)
        val chunkProvider = FixedLengthChunkProvider(bytes.inputStream(), 16)
        assertEquals(expected1, chunkProvider.nextChunk())
        assertEquals(expected2, chunkProvider.nextChunk())
        assertEquals(ByteString.EMPTY, chunkProvider.nextChunk())
        chunkProvider.close()
    }
}
