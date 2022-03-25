package info.skyblond.archivedag.actohw.bench;

import info.skyblond.archivedag.actohw.RobinKarpSlicer;
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
// Output 73536 files: Total 17,987,055,752 bytes
// Time usage: 224.535 s
// Average speed: 92.8 MB/s
// Average block size: 239KB
// Expected bloc size: 256KB
// Block distribution (Top 30%):
//   + 510KB: 9268 (12.60%)
//   + 60KB: 4511 (6.13%)
//   + 70KB: 4273 (5.81%)
//   + 80KB: 3724 (5.06%)
//   + 90KB: 3279 (4.46%)
class RobinKarpSlicerRealWorldTest extends AbstractSlicerBenchmarkTest {
    private final Path workDir = Path.of("D:\\test\\robin-karp-blobs");

    @BeforeEach
    void setUp() {
        this.slicer = new RobinKarpSlicer(this.workDir,
                Multihash.Type.sha3_256, Multihash.Type.blake2b_256,
                (1 << 18) - 1, 0, 64 * 1024, 512 * 1024,
                32 * 1024 * 1024, this.executorService,
                1000007, 48);
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
