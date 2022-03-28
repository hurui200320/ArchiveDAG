package info.skyblond.archivedag.actohw;

import com.google.protobuf.ByteString;
import io.ipfs.multihash.Multihash;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.stream.Stream;

public class FixedSlicer extends AbstractSlicer {
    private final int blockSize;
    private final byte[] buffer;

    public FixedSlicer(
            Path workDir, Multihash.Type primaryHashType, Multihash.Type secondaryHashType,
            ExecutorService executorService, int blockSize
    ) {
        super(workDir, primaryHashType, secondaryHashType, executorService);
        this.blockSize = blockSize;
        this.buffer = new byte[this.blockSize];
    }

    @Override
    protected Stream<CompletableFuture<BlobDescriptor>> digestAsyncSafe(File file) throws Exception {
        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            Stream.Builder<CompletableFuture<BlobDescriptor>> result = Stream.builder();
            while (fileInputStream.available() != 0) {
                int readCount = fileInputStream.readNBytes(this.buffer, 0, this.blockSize);
                ByteString content = ByteString.copyFrom(this.buffer, 0, readCount);
                result.add(this.writeToBlobFileAsync(content));
            }
            return result.build();
        }
    }
}
