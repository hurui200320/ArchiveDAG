package info.skyblond.ariteg.chunking

import java.io.InputStream

/**
 * Factory to get a [ChunkProvider] from a given stream.
 * */
interface ChunkProviderFactory {
    fun newInstance(inputStream: InputStream): ChunkProvider
}
