package info.skyblond.archivedag.actohw;

import com.google.protobuf.ByteString;
import io.ipfs.multihash.Multihash;

import java.io.IOException;
import java.nio.file.Path;

public class FixedSlicer extends AbstractSlicer {
    private final int blockSize;
    private final byte[] buffer;

    protected FixedSlicer(Path workDir, Multihash.Type hashType, int blockSize) {
        super(workDir, hashType);
        this.blockSize = blockSize;
        this.buffer = new byte[this.blockSize];
    }

    @Override
    protected BlobDescriptor nextChunk() {
        try {
            int readCount = this.inputStream.readNBytes(this.buffer, 0, this.blockSize);
            ByteString content = ByteString.copyFrom(this.buffer, 0, readCount);
            return this.writeToBlobFile(content);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected boolean hasNextChunk() {
        try {
            return this.inputStream.available() != 0;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
