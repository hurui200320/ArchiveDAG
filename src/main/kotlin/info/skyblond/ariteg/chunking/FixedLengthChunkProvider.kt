package info.skyblond.ariteg.chunking

import java.io.InputStream

class FixedLengthChunkProvider(
    inputStream: InputStream,
    chunkSizeInByte: Int
) : ChunkInputStreamAbstractProvider(inputStream) {

    private val buffer = ByteArray(chunkSizeInByte)

    override fun nextByteArray(): Pair<ByteArray, Int> {
        val readCount = inputStream.read(buffer)
        return if (readCount == -1)
            buffer to 0
        else
            buffer to readCount
    }
}
