package info.skyblond.archivedag.ariteg.multihash

import io.ipfs.multihash.Multihash
import java.io.InputStream

/**
 * A multihash provider will handle all hash calculation,
 * so we can upgrade to a new hash function by swapping this
 * provider, without changing other codes.
 *
 * This should be thread safe, unless the parameter is mutable and
 * shared across threads.
 * */
interface MultihashProvider {
    /**
     * What type this multihash provider give?
     * */
    fun getType(): Multihash.Type

    /**
     * Calculate the hash of the given byte array.
     * */
    fun digest(byteArray: ByteArray): Multihash

    /**
     * Calculate the hash of the given input stream.
     * This will read every byte in that stream,
     * and might be blocked if the stream is blocked.
     *
     * Note: The caller have to close the input stream.
     * This method won't close that for you.
     * */
    fun digest(inputStream: InputStream, bufferSize: Int = 4096): Multihash
}
