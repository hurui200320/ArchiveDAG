package info.skyblond.archivedag.actohw;

import com.google.protobuf.ByteString;
import io.ipfs.multihash.Multihash;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * Robin-Karp slicer. Use Rabin-Karp fingerprint to decide the chunk border.
 * This will try to slice the data at the same relative position, to leverage
 * data offsets.
 * <p>
 * E.g:
 * The data: 0123456789 -> 0123 456 789
 * Becomes: 0a123456789 -> 0a123 456 789
 * <p>
 * Rabin-Karp fingerprint: https://github.com/YADL/yadl/wiki/Rabin-Karp-for-Variable-Chunking
 */
public class RobinKarpSlicer extends AbstractSlicer {

    /**
     * Fingerprint mask.
     */
    private final int mask;

    /**
     * Predefined fingerprint.
     */
    private final int targetFingerprint;

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
     * Minimal chunk size.
     */
    private final int minChunkSize;

    /**
     * Maximum chunk size.
     */
    private final int maxChunkSize;

    /**
     * The prime number. Aka the `P`.
     */
    private final int p;

    /**
     * Read buffer for window.
     */
    private final int windowChannelBufferSize;

    /**
     * Thread pool for write operation
     */
    private final ExecutorService executorService;

    protected RobinKarpSlicer(
            Path workDir, Multihash.Type primaryHashType, Multihash.Type secondaryHashType,
            int fingerprintMask, int fingerprint, int windowSize, int minChunkSize, int maxChunkSize,
            int prime, int windowChannelBufferSize, ExecutorService executorService
    ) {
        super(workDir, primaryHashType, secondaryHashType);
        this.mask = fingerprintMask;
        this.targetFingerprint = fingerprint;
        this.windowSize = windowSize;
        this.windowBuffer = new int[this.windowSize];
        this.windowBufferPtr = 0;
        this.minChunkSize = minChunkSize;
        this.maxChunkSize = maxChunkSize;
        this.p = prime;
        this.windowChannelBufferSize = windowChannelBufferSize;
        this.executorService = executorService;
    }

    private FileChannel windowChannel;
    private ByteBuffer windowChannelBuffer;
    private int windowChannelBufferReadMax;
    private int windowChannelBufferReadCount;

    private int readNextWindowByte() throws IOException {
        if (this.windowChannelBufferReadCount >= this.windowChannelBufferReadMax) {
            this.windowChannelBufferReadMax = this.windowChannel.read(this.windowChannelBuffer);
            this.windowChannelBuffer.flip();
            this.windowChannelBufferReadCount = 0;
        }
        this.windowChannelBufferReadCount++;
        return this.windowChannelBuffer.get() & 0xFF;
    }

    private int pPowN; // P^N
    private int hash;

    private void initHash() throws IOException {
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
            // After the loop, first byte will time P^(N-1)
            // the second byte will time P^(N-2)
            // the last one will time 1
            this.hash *= this.p;
            this.hash += b;
            // calculate P^N
            this.pPowN *= this.p;
        }
    }

    /**
     * Calculate next hash.
     * <p>
     * hash(next) = { hash(prev) * P - P^N * outgoing + incoming } % m
     * <p>
     * The `% m` is performed by integer overflow.
     */
    private void calcNextHash() throws IOException {
        int inputByte = this.readNextWindowByte();
        this.hash *= this.p;
        this.hash -= this.pPowN * this.windowBuffer[this.windowBufferPtr];
        this.hash += inputByte;
        // save the input byte to buffer and move to next one
        this.windowBuffer[this.windowBufferPtr++] = inputByte;
        this.windowBufferPtr %= this.windowSize;
    }

    private FileInputStream chunkStream;
    private long lastUpdateMs = 0;
    private long processedBytes;
    private long totalBytes;

    private CompletableFuture<BlobDescriptor> makeNewChunk(int chunkSize) throws IOException {
        byte[] buffer = new byte[chunkSize];
        int readCount = this.chunkStream.read(buffer, 0, chunkSize);
        ByteString content = ByteString.copyFrom(buffer, 0, readCount);

        var future = CompletableFuture.supplyAsync(() -> {
            try {
                return this.writeToBlobFile(content);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }, this.executorService);

        long time = System.currentTimeMillis();
        double rate = chunkSize * 1000.0 / (Math.max(1, time - this.lastUpdateMs)) / 1024 / 1024;
        double progress = (this.processedBytes * 100.0 / this.totalBytes);
        System.out.println("Data rate: " + rate + " MB/s, " + progress + " %");
        this.lastUpdateMs = time;

        return future;
    }

    private RandomAccessFile reset(File file) throws FileNotFoundException {
        this.processedBytes = 0;
        this.totalBytes = file.length();

        if (this.totalBytes <= this.windowSize) {
            throw new IllegalArgumentException("Input file is too small");
        }

        this.chunkStream = new FileInputStream(file);
        RandomAccessFile r = new RandomAccessFile(file, "r");
        this.windowChannel = r.getChannel();
        this.windowChannelBuffer = ByteBuffer.allocate(this.windowChannelBufferSize);
        this.windowChannelBufferReadMax = 0;
        this.windowChannelBufferReadCount = this.windowChannelBufferReadMax;
        return r;
    }

    @Override
    public List<BlobDescriptor> digestSafe(File file) throws Exception {
        // prepare fields
        RandomAccessFile rFile = this.reset(file);
        // calculate init hash
        this.initHash();
        // the initialization will read `windowSize` bytes
        // count that in current chunk size
        int currentChunkSize = this.windowSize;

        List<CompletableFuture<BlobDescriptor>> result = new LinkedList<>();
        // while we haven't gone through the file
        while (this.processedBytes < this.totalBytes) {
            if (currentChunkSize >= this.minChunkSize) {
                // min chunk size reached, see if we can make a new chunk
                if ((this.hash & this.mask) == this.targetFingerprint) {
                    result.add(this.makeNewChunk(currentChunkSize));
                    this.processedBytes += currentChunkSize;
                    currentChunkSize = 0; // reset chunk size counter
                }
            }
            this.calcNextHash(); // read next hash
            currentChunkSize++;
            if (currentChunkSize >= this.maxChunkSize) {
                // max chunk size reached, we have to make a new chunk
                result.add(this.makeNewChunk(currentChunkSize));
                this.processedBytes += currentChunkSize;
                currentChunkSize = 0; // reset chunk size counter
            }
        }
        if (this.chunkStream.available() != 0) {
            // make the rest a chunk
            result.add(this.makeNewChunk(currentChunkSize));
        }

        this.windowChannel.close();
        rFile.close();
        this.chunkStream.close();

        return result.stream().map(it -> {
            try {
                return it.get();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).toList();
    }
}
