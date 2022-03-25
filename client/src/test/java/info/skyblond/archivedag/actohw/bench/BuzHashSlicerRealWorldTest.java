package info.skyblond.archivedag.actohw.bench;

import info.skyblond.archivedag.actohw.BuzHashSlicer;
import io.ipfs.multihash.Multihash;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;

// Input  5     files: Total 21,849,735,168 bytes
// Output 63080 files: Total 18,207,642,254 bytes
// Time usage: 234.675 s
// Average speed: 88.8 MB/s
// Average block size: 282KB
// Expected bloc size: 256KB
// Block distribution (Top 30%):
//   + 510KB: 11971 (18.98%)
//   + 80KB: 2231 (3.54%)
//   + 70KB: 2210 (3.50%)
//   + 90KB: 2151 (3.41%)
//   + 100KB: 2007 (3.18%)
class BuzHashSlicerRealWorldTest extends AbstractSlicerBenchmarkTest {
    private final Path workDir = Path.of("D:\\test\\buz-hash-blobs");

    @BeforeEach
    void setUp() {
        this.slicer = new BuzHashSlicer(this.workDir,
                Multihash.Type.sha3_256, Multihash.Type.blake2b_256,
                (1 << 18) - 1, 0, 64 * 1024, 512 * 1024,
                32 * 1024 * 1024, this.executorService,
                BuzHashSlicer.DEFAULT_HASHTABLE, 48);
    }

    @Test
    void test() throws IOException {
        this.deleteDirectory(this.workDir.toFile());
        Files.createDirectories(this.workDir);
        File dir = new File("D:\\test\\ISO");
        Arrays.stream(Objects.requireNonNull(dir.listFiles())).forEach(f -> {
            System.gc();
            System.out.println("Sliced into " + this.sliceFile(f).size() + " chunks");
            System.gc();
        });
    }

    @Test
    void count() throws IOException {
        this.countChunk(this.workDir);
    }
}
