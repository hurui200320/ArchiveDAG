package info.skyblond.archivedag.apwiho.services;

import info.skyblond.archivedag.actohw.FixedSlicer;
import info.skyblond.archivedag.actohw.RabinKarpSlicer;
import io.ipfs.multihash.Multihash;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class SlicerService implements AutoCloseable {
    private static final SlicerService ourInstance = new SlicerService();
    private final Logger logger = LoggerFactory.getLogger("SlicerService");

    public static SlicerService getInstance() {
        return ourInstance;
    }

    private SlicerService() {
    }

    // Here we use caller run to slow down the writing request
    // But then there might be multiple threads try to write the same file
    // It's rare, but the file will be corrupted, and then failed when upload
    // During the upload, the program will check the content and throw error if hash not match
    private final ExecutorService executorService = new ThreadPoolExecutor(1, 1,
            0L, TimeUnit.MILLISECONDS,
            // assuming each task write 200KB, then 20K is roughly 4GB
            new LinkedBlockingQueue<>(20_000), new ThreadPoolExecutor.CallerRunsPolicy());

    private final int MIN_CHUNK_SIZE = 64 * 1024;

    public long getMinChunkSize() {
        return this.MIN_CHUNK_SIZE;
    }

    public RabinKarpSlicer getDynamicSlicer(Path workDir, Multihash.Type primaryHashType, Multihash.Type secondaryHashType) {
        return new RabinKarpSlicer(workDir, primaryHashType, secondaryHashType,
                (1 << 18) - 1, 0, this.MIN_CHUNK_SIZE, 4 * 1024 * 1024,
                32 * 1024 * 1024, this.executorService,
                1821497, 48);
    }

    public FixedSlicer getFixedSlicer(Path workDir, Multihash.Type primaryHashType, Multihash.Type secondaryHashType) {
        return new FixedSlicer(workDir, primaryHashType, secondaryHashType, this.executorService, this.MIN_CHUNK_SIZE);
    }

    @Override
    public void close() throws Exception {
        this.executorService.shutdown();
        var alert = DialogService.getInstance().showWaitingDialog("Closing thread pool...");
        while (!this.executorService.awaitTermination(500, TimeUnit.MILLISECONDS)) {
            this.logger.info("Waiting for thread pool...");
        }
        alert.close();
    }
}
