package info.skyblond.archivedag.actohw;

import com.google.protobuf.ByteString;
import io.ipfs.multihash.Multihash;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

public class FixedSlicer extends AbstractSlicer {
    private final int blockSize;
    private final byte[] buffer;

    protected FixedSlicer(Path workDir, Multihash.Type primaryHashType, Multihash.Type secondaryHashType, int blockSize) {
        super(workDir, primaryHashType, secondaryHashType);
        this.blockSize = blockSize;
        this.buffer = new byte[this.blockSize];
    }

    @Override
    public List<BlobDescriptor> digestSafe(File file) throws Exception {
        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            List<BlobDescriptor> result = new LinkedList<>();
            while (fileInputStream.available() != 0) {
                int readCount = fileInputStream.readNBytes(this.buffer, 0, this.blockSize);
                ByteString content = ByteString.copyFrom(this.buffer, 0, readCount);
                result.add(this.writeToBlobFile(content));
            }
            return result;
        }
    }
}
