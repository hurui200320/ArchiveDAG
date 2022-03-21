package info.skyblond.archivedag.actohw;

import com.google.protobuf.ByteString;
import info.skyblond.archivedag.ariteg.multihash.MultihashProviders;
import info.skyblond.archivedag.ariteg.protos.AritegBlobObject;
import io.ipfs.multihash.Multihash;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

public abstract class AbstractSlicer implements Slicer {
    private final Path workDir;
    private final Multihash.Type hashType;
    protected InputStream inputStream;

    // Use a single thread executor here.
    protected AbstractSlicer(Path workDir, Multihash.Type hashType) {
        this.workDir = workDir;
        this.hashType = hashType;
    }

    private File multihashToFile(Multihash multihash) {
        String base58 = multihash.toBase58();
        String prefix = base58.substring(0, 8);
        File baseDir = new File(this.workDir.toFile(), prefix);
        if (!baseDir.exists()) {
            baseDir.mkdirs();
        }
        return new File(baseDir, base58);
    }

    protected BlobDescriptor writeToBlobFile(ByteString content) throws IOException {
        var blob = AritegBlobObject.newBuilder()
                .setData(content)
                .build();
        var multihash = MultihashProviders.fromMultihashType(this.hashType).digest(blob.toByteArray());
        File f = this.multihashToFile(multihash);
        if (!f.exists()) {
            // During the write process, the file can be corrupted if other threads are writing,
            // or some thread is reading, they will read half content
            Files.write(f.toPath(), blob.toByteArray());
        } else {
            try (InputStream is = new FileInputStream(f)) {
                MultihashProviders.mustMatch(multihash, is);
            }
        }
        return new BlobDescriptor(multihash, f);
    }

    protected abstract BlobDescriptor nextChunk();

    protected abstract boolean hasNextChunk();

    @Override
    public List<BlobDescriptor> digest(InputStream input) {
        this.inputStream = input;
        List<BlobDescriptor> result = new LinkedList<>();
        while (this.hasNextChunk()) {
            result.add(this.nextChunk());
        }
        return result;
    }

}
