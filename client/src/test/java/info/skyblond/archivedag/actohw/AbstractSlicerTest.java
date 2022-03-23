package info.skyblond.archivedag.actohw;

import com.google.protobuf.ByteString;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.stream.Stream;

public abstract class AbstractSlicerTest {
    protected Path tempWorkDir;
    protected Slicer slicer;
    protected Random random;

    private List<File> tempFileList = new LinkedList<>();

    @BeforeEach
    void setup() throws IOException {
        this.random = new Random();
        this.tempWorkDir = Files.createTempDirectory("actohw-test");
        System.out.println("Using temp dir: " + this.tempWorkDir);
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

    @AfterEach
    void tearDown() throws IOException {
        try (Stream<Path> walk = Files.walk(this.tempWorkDir)) {
            walk.map(Path::toFile).forEach(this::deleteDirectory);
        }
        for (File file : this.tempFileList) {
            this.deleteDirectory(file);
        }
    }

    protected File writeTempFile(byte[] content) throws IOException {
        File file = File.createTempFile("actohw-test", ".blob");
        this.tempFileList.add(file);
        Files.write(file.toPath(), content);
        return file;
    }

    protected List<BlobDescriptor> testSlice(byte[] content) throws IOException {
        ByteString target = null;
        List<BlobDescriptor> result;

        result = this.slicer.digest(this.writeTempFile(content));
        System.out.println(result);
        for (BlobDescriptor blobDescriptor : result) {
            var b = blobDescriptor.readBlob();
            if (target == null) {
                target = b.getData();
            } else {
                target = target.concat(b.getData());
            }
        }

        Assertions.assertNotNull(target);
        Assertions.assertArrayEquals(content, target.toByteArray());
        return result;
    }

    protected void testSliceRandom(int size) throws IOException {
        var content = new byte[size];
        this.random.nextBytes(content);
        this.testSlice(content);
    }
}
