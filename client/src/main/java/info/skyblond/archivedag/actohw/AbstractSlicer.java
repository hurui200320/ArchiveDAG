package info.skyblond.archivedag.actohw;

import com.google.protobuf.ByteString;
import info.skyblond.archivedag.ariteg.multihash.MultihashProvider;
import info.skyblond.archivedag.ariteg.multihash.MultihashProviders;
import info.skyblond.archivedag.ariteg.protos.AritegBlobObject;
import io.ipfs.multihash.Multihash;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.stream.Stream;

public abstract class AbstractSlicer implements Slicer {
    private final Path workDir;
    private final MultihashProvider primaryHashProvider;
    private final MultihashProvider secondaryHashProvider;
    private final ExecutorService executorService;

    // Use a single thread executor here.
    protected AbstractSlicer(Path workDir, Multihash.Type primaryHashType, Multihash.Type secondaryHashType, ExecutorService executorService) {
        this.workDir = workDir;
        this.primaryHashProvider = MultihashProviders.fromMultihashType(primaryHashType);
        this.secondaryHashProvider = MultihashProviders.fromMultihashType(secondaryHashType);
        this.executorService = executorService;
    }

    private File multihashToFile(Multihash multihash) {
        String base58 = multihash.toBase58();
        String prefix = base58.substring(0, 6);
        File baseDir = new File(this.workDir.toFile(), prefix);
        if (!baseDir.exists() && !baseDir.mkdirs()) {
            throw new RuntimeException("Failed to create dir: " + baseDir);
        }
        return new File(baseDir, base58);
    }

    protected BlobDescriptor writeToBlobFile(ByteString content) throws IOException {
        var blob = AritegBlobObject.newBuilder()
                .setData(content)
                .build();
        var multihash = this.primaryHashProvider.digest(blob.toByteArray());
        File f = this.multihashToFile(multihash);
        if (!f.exists()) {
            // During the write process, the file can be corrupted if other threads are writing,
            // or some thread is reading, they will read half content
            Files.write(f.toPath(), blob.toByteArray());
        } else {
            // if the file exists, check the secondary hash to prevent collision
            var secondaryHash = this.secondaryHashProvider.digest(blob.toByteArray());
            try (InputStream is = new FileInputStream(f)) {
                MultihashProviders.mustMatch(secondaryHash, is);
            }
        }
        return new BlobDescriptor(multihash, f);
    }

    protected CompletableFuture<BlobDescriptor> writeToBlobFileAsync(ByteString content) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return this.writeToBlobFile(content);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }, this.executorService);
    }

    @Override
    final public List<BlobDescriptor> digest(File file) {
        return this.digestAsync(file).map(it -> {
            try {
                return it.get();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).toList();
    }


    protected abstract Stream<CompletableFuture<BlobDescriptor>> digestAsyncSafe(File file) throws Exception;

    @Override
    public Stream<CompletableFuture<BlobDescriptor>> digestAsync(File file) {
        try {
            return this.digestAsyncSafe(file);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
