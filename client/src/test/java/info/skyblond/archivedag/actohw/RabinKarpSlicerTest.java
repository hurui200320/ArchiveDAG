package info.skyblond.archivedag.actohw;

import io.ipfs.multihash.Multihash;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

class RabinKarpSlicerTest extends AbstractSlicerTest {

    @BeforeEach
    void setUp() {
        this.slicer = new RabinKarpSlicer(this.tempWorkDir,
                Multihash.Type.sha3_256, Multihash.Type.blake2b_256,
                (1 << 10) - 1, 0, 64, 128,
                32 * 1024 * 1024, this.executorService,
                1821497, 48);
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
