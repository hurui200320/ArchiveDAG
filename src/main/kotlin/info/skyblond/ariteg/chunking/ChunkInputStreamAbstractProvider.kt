package info.skyblond.ariteg.chunking

import com.google.protobuf.ByteString
import java.io.InputStream

abstract class ChunkInputStreamAbstractProvider(
    protected val inputStream: InputStream
) : ChunkProvider, AutoCloseable {

    /**
     * Get next chunk from input stream.
     * @return Pair<buffer, length>, data start from 0.
     * */
    protected abstract fun nextByteArray(): Pair<ByteArray, Int>

    override fun nextChunk(): ByteString {
        val (data, length) = nextByteArray()
        return if (length == 0)
            ByteString.EMPTY
        else
            ByteString.copyFrom(data, 0, length)
    }

    override fun close() {
        inputStream.close()
    }
}
