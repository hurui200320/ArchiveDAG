package info.skyblond.archivedag.actohw.bench;

import info.skyblond.archivedag.actohw.BlobDescriptor;
import info.skyblond.archivedag.actohw.Slicer;
import javafx.util.Pair;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// This bench will try to found best window size,
public abstract class AbstractSlicerBenchmarkTest {
    protected Slicer slicer;
    protected ExecutorService executorService;

    @BeforeEach
    void prepare() {
        this.executorService = Executors.newSingleThreadExecutor();
    }

    @AfterEach
    void cleanUp() {
        this.executorService.shutdown();
    }

    public void deleteDirectory(File file) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File f : files) {
                    this.deleteDirectory(f);
                }
            }
        }
        if (!file.delete()) {
            System.err.println("Failed to delete " + file);
        }
    }

    protected List<BlobDescriptor> sliceFile(File f) {
        System.out.println("Start slicing file: " + f.getName());
        long startTime = System.currentTimeMillis();
        var result = this.slicer.digest(f);
        long endTime = System.currentTimeMillis();
        System.out.println("Sliced file " + f.getName() + ". Time usage: " + (endTime - startTime) + " ms");
        return result;
    }


    protected final HashMap<String, Integer> sizeCounter = new HashMap<>();
    protected int blobCounter = 0;

    private void countPath(Path root) throws IOException {
        Files.walk(root).filter(p -> p != root).forEach(p -> {
            if (!Files.isDirectory(p)) {
                // is file, count it
                this.blobCounter++;
                if (this.blobCounter % 1000 == 0) {
                    System.out.print(".");
                    if (this.blobCounter % 80000 == 0) {
                        System.out.println();
                    }
                }
                try {
                    var size = Files.size(p) / 1024 / 10; // floor to 10KB
                    var key = String.format("%d0KB", size);
                    if (this.sizeCounter.containsKey(key)) {
                        this.sizeCounter.put(key, this.sizeCounter.get(key) + 1);
                    } else {
                        this.sizeCounter.put(key, 1);
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    protected void countChunk(Path workDir) throws IOException {
        Assumptions.assumeTrue(workDir.toFile().exists());
        this.blobCounter = 0;
        this.sizeCounter.clear();
        this.countPath(workDir);
        System.out.println();
        List<Pair<Integer, String>> list = new LinkedList<>();
        this.sizeCounter.forEach((k, v) -> list.add(new Pair<>(v, k)));
        list.sort(Comparator.comparingInt(Pair::getKey));
        Collections.reverse(list);
        System.out.println(this.blobCounter + " blobs in total.");
        list.forEach(p -> {
            double rate = p.getKey() * 100.0 / this.blobCounter;
            System.out.println("//   + " + p.getValue() + ": " + p.getKey() + " (" + String.format("%.2f", rate) + "%)");
        });
    }
}
