package info.skyblond.archivedag.actohw;

import io.ipfs.multihash.Multihash;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;

/**
 * Rabin-Karp slicer. Use Rabin-Karp fingerprint to decide the chunk border.
 * This will try to slice the data at the same relative position, to leverage
 * data offsets.
 * <p>
 * E.g:
 * The data: 0123456789 -> 0123 456 789
 * Becomes: 0a123456789 -> 0a123 456 789
 * <p>
 * Rabin-Karp fingerprint: <a href="https://github.com/YADL/yadl/wiki/Rabin-Karp-for-Variable-Chunking">https://github.com/YADL/yadl/wiki/Rabin-Karp-for-Variable-Chunking</a>
 */
public class RabinKarpSlicer extends RollingHashSlicer {
    /**
     * Window size, aka the `N`.
     */
    private final int windowSize; // windowSize, aka the `N`

    /**
     * Window buffer. For computation, it's int rather than byte.
     */
    private final int[] windowBuffer;

    /**
     * Window buffer pointer. Indicate current position.
     */
    private int windowBufferPtr;

    /**
     * The prime number. Aka the `P`.
     */
    private final int p;

    public RabinKarpSlicer(
            Path workDir, Multihash.Type primaryHashType, Multihash.Type secondaryHashType,
            int fingerprintMask, int fingerprint, int minChunkSize, int maxChunkSize,
            int windowChannelBufferSize, ExecutorService executorService,
            int prime, int windowSize
    ) {
        super(workDir, primaryHashType, secondaryHashType,
                fingerprintMask, fingerprint, minChunkSize, maxChunkSize,
                windowChannelBufferSize, executorService
        );
        this.windowSize = windowSize;
        this.windowBuffer = new int[this.windowSize];
        this.windowBufferPtr = 0;
        this.p = prime;
    }


    private int pPowN; // P^N

    @Override
    protected int initHash() throws IOException {
        this.hash = 0;
        this.windowBufferPtr = 0;
        this.pPowN = 1; // initial: P^0;
        // hash = P^(N-1)*b[0] + P^(N-2)*b[1] + ... + P * b[N-2] + b[N-1]
        // ---> = ((((...((b[0] * P + b[1]) * P) * P + ...) * P + b[N-1]
        // The b[0] will get P^(N-1), b[1] will get P^[N-2]
        // The b[N-2] will get P, and b[N-1] will get P^0
        for (int i = 0; i < this.windowSize; i++) {
            int b = this.readNextWindowByte();
            // save the content
            this.windowBuffer[this.windowBufferPtr++] = b;
            this.windowBufferPtr %= this.windowSize;
            // hash' = hash * P + b
            this.hash = this.hash * this.p + b;
            // calculate P^N
            this.pPowN *= this.p;
        }
        return this.windowSize;
    }

    /**
     * Calculate next hash.
     * <p>
     * hash(next) = { hash(prev) * P - P^N * outgoing + incoming } % m
     * <p>
     * The `% m` is performed by integer overflow.
     */
    @Override
    protected void calcNextHash() throws IOException {
        int in = this.readNextWindowByte();
        // hash' = hash * P - P^N * out + in
        this.hash = this.hash * this.p - this.pPowN * this.windowBuffer[this.windowBufferPtr] + in;
        // save the input byte to buffer and move to next one
        this.windowBuffer[this.windowBufferPtr++] = in;
        this.windowBufferPtr %= this.windowSize;
    }

    @Override
    protected void reset(File file) {
        if (this.getTotalBytes() <= this.windowSize) {
            throw new IllegalArgumentException("Input file is too small");
        }
    }
}
