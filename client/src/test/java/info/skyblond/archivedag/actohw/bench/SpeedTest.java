package info.skyblond.archivedag.actohw.bench;

import info.skyblond.archivedag.actohw.BuzHashSlicer;
import info.skyblond.archivedag.actohw.FixedSlicer;
import info.skyblond.archivedag.actohw.RabinKarpSlicer;
import info.skyblond.archivedag.actohw.Slicer;
import io.ipfs.multihash.Multihash;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Disabled
public class SpeedTest {
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Path workDir = Path.of("R:\\test-blobs");
    private final List<Slicer> slicers = List.of(
            new FixedSlicer(this.workDir,
                    Multihash.Type.sha3_512, Multihash.Type.blake2b_512,
                    this.executorService, 256 * 1024 // 256KB
            ),
            new RabinKarpSlicer(this.workDir,
                    Multihash.Type.sha3_256, Multihash.Type.blake2b_256,
                    (1 << 18) - 1, 0,
                    64 * 1024, 512 * 1024,
                    32 * 1024 * 1024, this.executorService,
                    1821497, 48
            ),
            new BuzHashSlicer(this.workDir,
                    Multihash.Type.sha3_256, Multihash.Type.blake2b_256,
                    (1 << 18) - 1, 0,
                    64 * 1024, 512 * 1024,
                    32 * 1024 * 1024, this.executorService,
                    BuzHashSlicer.DEFAULT_HASHTABLE, 48
            )
    );

    private void deleteR(File file) {
        if (!file.exists()) {
            return;
        }
        File[] files = file.listFiles();
        if (files != null) {
            for (File f : files) {
                this.deleteR(f);
            }
        }
        if (!file.delete()) {
            this.deleteR(file);
        }
    }

    private void test(Slicer slicer, File testFile) {
        this.deleteR(this.workDir.toFile());
        Assertions.assertTrue(this.workDir.toFile().mkdirs());
        System.out.println("Start testing...");
        long startTime = System.currentTimeMillis();
        var result = slicer.digestAsync(testFile);
        long midTime = System.currentTimeMillis();
        result.forEach(it -> {
            try {
                it.get();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        long endTime = System.currentTimeMillis();

        System.out.println("Slicer: " + slicer.getClass().getName());
        System.out.println("Slicing: " + (midTime - startTime) + "ms");
        System.out.println("Total: " + (endTime - startTime) + "ms");
        System.out.println();
    }

    @Test
    void doTest() {
        File testFile = new File("R:\\test");
        this.slicers.forEach(it -> this.test(it, testFile));
    }
}
