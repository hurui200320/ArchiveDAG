package info.skyblond.archivedag.actohw.bench;

import info.skyblond.archivedag.actohw.*;
import io.ipfs.multihash.Multihash;
import javafx.util.Pair;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Disabled
public class EfficiencyTest {
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final List<AbstractSlicer> slicers = List.of(
            new FixedSlicer(Path.of("D:\\test\\fixed-blobs"),
                    Multihash.Type.sha3_512, Multihash.Type.blake2b_512,
                    this.executorService, 256 * 1024 // 256KB
            ),
            new RabinKarpSlicer(Path.of("D:\\test\\rabin-karp-blobs"),
                    Multihash.Type.sha3_256, Multihash.Type.blake2b_256,
                    (1 << 18) - 1, 0,
                    64 * 1024, 512 * 1024,
                    32 * 1024 * 1024, this.executorService,
                    1821497, 48
            ),
            new BuzHashSlicer(Path.of("D:\\test\\buz-hash-blobs"),
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

    private void test(AbstractSlicer slicer, File testDir) throws IOException {
        System.out.println("Deleting old run...");
        this.deleteR(slicer.getWorkDir().toFile());
        Assertions.assertTrue(slicer.getWorkDir().toFile().mkdirs());
        System.out.println("Start slicing...");
        LinkedList<BlobDescriptor> results = new LinkedList<>();

        Arrays.stream(Objects.requireNonNull(testDir.listFiles())).forEach(f -> {
            System.gc();
            results.addAll(slicer.digest(f));
            System.gc();
        });

        System.out.println("Analyzing result...");
        HashSet<Multihash> multihashHashSet = new HashSet<>();
        // 10K count
        HashMap<String, Integer> sizeCounter = new HashMap<>();
        long counter = 0L;
        for (BlobDescriptor blob : results) {
            counter++;
            if (!multihashHashSet.contains(blob.primaryHash())) {
                multihashHashSet.add(blob.primaryHash());
                var size = Files.size(blob.file().toPath()) / 1024 / 10; // floor to 10KB
                var key = String.format("%d0KB", size);
                if (sizeCounter.containsKey(key)) {
                    sizeCounter.put(key, sizeCounter.get(key) + 1);
                } else {
                    sizeCounter.put(key, 1);
                }
            }
        }

        System.out.println("Slicer: " + slicer.getClass().getName());
        System.out.println("Generated " + counter + " blobs in total");
        System.out.println("There are " + multihashHashSet.size() + " unique blobs");
        System.out.println("The blob size distro is...");

        List<Pair<Integer, String>> list = new LinkedList<>();
        sizeCounter.forEach((k, v) -> list.add(new Pair<>(v, k)));
        list.sort(Comparator.comparingInt(Pair::getKey));
        Collections.reverse(list);
        long finalCounter = counter;
        list.forEach(p -> {
            double rate = p.getKey() * 100.0 / finalCounter;
            System.out.println("" + p.getValue() + ": " + p.getKey() + " (" + String.format("%.2f", rate) + "%)");
        });

        System.out.println();
    }

    @Test
    void doTest() {
        File testFile = new File("D:\\test\\ISO");
        this.slicers
                .forEach(it -> {
                    try {
                        this.test(it, testFile);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
    }
}
