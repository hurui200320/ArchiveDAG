package info.skyblond.archivedag.actohw;

import com.google.protobuf.ByteString;
import io.ipfs.multihash.Multihash;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.stream.Stream;

public abstract class RollingHashSlicer extends AbstractSlicer {
    /**
     * Fingerprint mask.
     */
    private final int mask;

    /**
     * Predefined fingerprint.
     */
    private final int targetFingerprint;

    /**
     * Calculated hash
     */
    protected int hash;

    /**
     * Minimal chunk size.
     */
    private final int minChunkSize;

    /**
     * Maximum chunk size.
     */
    private final int maxChunkSize;

    /**
     * Read buffer for window.
     */
    private final int windowChannelBufferSize;

    protected RollingHashSlicer(
            Path workDir, Multihash.Type primaryHashType, Multihash.Type secondaryHashType,
            int fingerprintMask, int fingerprint, int minChunkSize, int maxChunkSize,
            int windowChannelBufferSize, ExecutorService executorService
    ) {
        super(workDir, primaryHashType, secondaryHashType, executorService);
        this.mask = fingerprintMask;
        this.targetFingerprint = fingerprint;
        this.minChunkSize = minChunkSize;
        this.maxChunkSize = maxChunkSize;
        this.windowChannelBufferSize = windowChannelBufferSize;
    }

    /**
     * Return ture if masked hash equals to the target fingerprint.
     */
    protected boolean isFingerprintMatch() {
        return (this.hash & this.mask) == this.targetFingerprint;
    }

    /**
     * Reset to prepare for digesting new file.
     */
    protected abstract void reset(File file) throws Exception;

    private FileInputStream chunkStream;
    private FileChannel windowChannel;
    private ByteBuffer windowChannelBuffer;
    private int windowChannelBufferReadMax;
    private int windowChannelBufferReadCount;

    /**
     * Init file read channel, chunk stream.
     */
    private RandomAccessFile prepare(File file) throws Exception {
        this.processedBytes = 0;
        this.totalBytes = file.length();
        this.chunkStream = new FileInputStream(file);
        RandomAccessFile r = new RandomAccessFile(file, "r");
        this.windowChannel = r.getChannel();
        this.windowChannelBuffer = ByteBuffer.allocate(this.windowChannelBufferSize);
        this.windowChannelBufferReadMax = 0;
        this.windowChannelBufferReadCount = this.windowChannelBufferReadMax;
        this.reset(file);
        return r;
    }

    protected int readNextWindowByte() throws IOException {
        if (this.windowChannelBufferReadCount >= this.windowChannelBufferReadMax) {
            this.windowChannelBufferReadMax = this.windowChannel.read(this.windowChannelBuffer);
            this.windowChannelBuffer.flip();
            this.windowChannelBufferReadCount = 0;
        }
        this.windowChannelBufferReadCount++;
        return this.windowChannelBuffer.get() & 0xFF;
    }

    private long processedBytes;

    protected long getProcessedBytes() {
        return this.processedBytes;
    }

    private long totalBytes;

    protected long getTotalBytes() {
        return this.totalBytes;
    }

    /**
     * Called when new chunk is generated. Used for log statistic.
     */
    protected void onNewChunk(int chunkSize) {
    }

    /**
     * Generate a new chunk.
     */
    private CompletableFuture<BlobDescriptor> makeNewChunk(int chunkSize) throws IOException {
        byte[] buffer = new byte[chunkSize];
        int readCount = this.chunkStream.read(buffer, 0, chunkSize);
        ByteString content = ByteString.copyFrom(buffer, 0, readCount);
        this.onNewChunk(chunkSize);
        return this.writeToBlobFileAsync(content);
    }

    /**
     * Init the window and calculate the hash.
     * Return the processed byte counts (normally is the window size)
     */
    protected abstract int initHash() throws Exception;

    /**
     * Calculate next rolling hash.
     * Save the result to {@link RollingHashSlicer#hash}
     */
    protected abstract void calcNextHash() throws Exception;

    /**
     * Called when slicing is finished. Used for subclass to close resources.
     */
    protected abstract void onFinished() throws Exception;

    /**
     * Digest new file.
     */
    @Override
    public Stream<CompletableFuture<BlobDescriptor>> digestAsyncSafe(File file) throws Exception {
        // prepare fields
        RandomAccessFile rFile = this.prepare(file);
        // calculate init hash

        // the initialization will read `windowSize` bytes
        // count that in current chunk size
        int currentChunkSize = this.initHash();

        Stream.Builder<CompletableFuture<BlobDescriptor>> result = Stream.builder();
        // while we haven't gone through the file
        while (this.processedBytes < this.totalBytes) {
            if (currentChunkSize >= this.minChunkSize) {
                // min chunk size reached, see if we can make a new chunk
                if (this.isFingerprintMatch()) {
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
        this.onFinished();

        return result.build();
    }
}
