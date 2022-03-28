package info.skyblond.archivedag.apwiho.services;

import info.skyblond.archivedag.actohw.FixedSlicer;
import info.skyblond.archivedag.actohw.RobinKarpSlicer;
import io.ipfs.multihash.Multihash;

import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class SlicerService implements AutoCloseable {
    private static final SlicerService ourInstance = new SlicerService();

    public static SlicerService getInstance() {
        return ourInstance;
    }

    private SlicerService() {
    }

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final int MIN_CHUNK_SIZE = 64 * 1024;

    public long getMinChunkSize() {
        return this.MIN_CHUNK_SIZE;
    }

    public RobinKarpSlicer getDynamicSlicer(Path workDir, Multihash.Type primaryHashType, Multihash.Type secondaryHashType) {
        return new RobinKarpSlicer(workDir, primaryHashType, secondaryHashType,
                (1 << 18) - 1, 0, this.MIN_CHUNK_SIZE, 512 * 1024,
                32 * 1024 * 1024, this.executorService,
                1000007, 48);
    }

    public FixedSlicer getFixedSlicer(Path workDir, Multihash.Type primaryHashType, Multihash.Type secondaryHashType) {
        return new FixedSlicer(workDir, primaryHashType, secondaryHashType, this.executorService, this.MIN_CHUNK_SIZE);
    }

    @Override
    public void close() throws Exception {
        this.executorService.shutdown();
        var alert = DialogService.getInstance().showWaitingDialog("Closing thread pool...");
        while (!this.executorService.awaitTermination(500, TimeUnit.MILLISECONDS)) {
            System.out.println("Waiting for thread pool...");
        }
        alert.close();
    }
}
