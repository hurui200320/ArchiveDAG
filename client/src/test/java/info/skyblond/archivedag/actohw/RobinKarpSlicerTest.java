package info.skyblond.archivedag.actohw;

import io.ipfs.multihash.Multihash;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class RobinKarpSlicerTest extends AbstractSlicerTest {

    private final ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    @BeforeEach
    void setUp() {
        this.slicer = new RobinKarpSlicer(this.tempWorkDir,
                Multihash.Type.sha3_256, Multihash.Type.blake2b_256,
                (1 << 10) - 1, 0, 48,
                64, 128, 31,
                32 * 1024 * 1024,
                this.executorService);
    }

    @Override
    @AfterEach
    void tearDown() throws IOException {
        super.tearDown();
        this.executorService.shutdown();
    }

    @Test
    void testSmall() throws IOException {
        this.testSliceRandom(63);
    }

    @Test
    void testMiddle() throws IOException {
        this.testSliceRandom(200);
    }

    @Test
    void testBig() throws IOException {
        this.testSliceRandom(1050);
    }

}
