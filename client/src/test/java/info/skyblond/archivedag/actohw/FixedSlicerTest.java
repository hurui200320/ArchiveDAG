package info.skyblond.archivedag.actohw;

import io.ipfs.multihash.Multihash;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

class FixedSlicerTest extends AbstractSlicerTest {

    @BeforeEach
    void setUp() {
        this.slicer = new FixedSlicer(this.tempWorkDir, Multihash.Type.sha3_256, 128);
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

    @Test
    void testReuse() throws IOException {
        var content = new byte[8193];
        var result = this.testSlice(content).stream()
                .map(BlobDescriptor::multihash).distinct().toList();
        System.out.println(result);
        Assertions.assertEquals(2, result.size());
    }


}
