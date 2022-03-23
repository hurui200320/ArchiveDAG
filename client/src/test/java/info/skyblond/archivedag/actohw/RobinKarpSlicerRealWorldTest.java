package info.skyblond.archivedag.actohw;

import io.ipfs.multihash.Multihash;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class RobinKarpSlicerRealWorldTest {

    private final ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    private final Path workDir = Path.of("D:\\test\\robin-karp-blobs");
    private final RobinKarpSlicer slicer = new RobinKarpSlicer(this.workDir,
            Multihash.Type.sha3_256, Multihash.Type.blake2b_256,
            (1 << 18) - 1, 0, 384,
            64 * 1024, 8 * 1024 * 1024, 31,
            32 * 1024 * 1024,
            this.executorService);

    @BeforeEach
    void setUp() throws IOException {
        Files.createDirectories(this.workDir);
    }

    @AfterEach
    void tearDown() {
        this.executorService.shutdown();
    }

    @Test
    void test() {
        File dir = new File("D:\\test");
        long startTime = System.currentTimeMillis();
        var result1 = this.slicer.digest(new File(dir, "[Kamigami] Kurenai no Buta Porco Rosso 1992 [BD x264 1080p DTS-HD(Jap,Man,Can,Eng,Fre,Ger,Fin,Kor) Sub×6].mkv"));
        var result2 = this.slicer.digest(new File(dir, "[Kamigami] Laputa Castle in the Sky [BD x264 1080p DTS-HD(Jap,Man,Can,Eng,Fre,Ger,Kor) Sub×9].mkv"));
        long endTime = System.currentTimeMillis();
        System.out.println(result1.size());
        System.out.println(result2.size());
        System.out.println("Time usage: " + (endTime - startTime) + " ms");

    }

}
