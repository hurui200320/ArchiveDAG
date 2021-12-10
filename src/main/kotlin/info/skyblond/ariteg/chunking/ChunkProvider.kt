package info.skyblond.ariteg.chunking

import com.google.protobuf.ByteString

/**
 * This is an interface for Content defined chunking algorithms.
 * By default, each blob has same size when chopping down a stream
 * of data. However, you might want to chop the data based on their
 * content to get a more efficient deduplication.
 * */
interface ChunkProvider {
    /**
     * Offer the next chunk of data.
     * Return [ByteString.EMPTY] means the end.
     * */
    fun nextChunk(): ByteString
}
