package info.skyblond.archivedag.actohw.bench;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Random;

@Disabled
public class PrepareFileTest {
    @Test
    void createFile() throws IOException {
        // 4GB in total
        long size = 4L * 1024 * 1024 * 1024;
        File file = new File("R:\\test");
        // 32MB buffer
        byte[] buffer = new byte[32 * 1024 * 1024];
        // SHA256: 68DBB15A9F111FE4BFB979E936F47C2EF32642DFA055481B7587D129EF9FC107
        Random random = new Random(18101130109L);
        long counter = 0;
        try (FileOutputStream fileOutputStream = new FileOutputStream(file)) {
            while (counter < size) {
                random.nextBytes(buffer);
                long remain = size - counter;
                long writeSize = Math.min(remain, buffer.length);
                fileOutputStream.write(buffer, 0, (int) writeSize);
                counter += writeSize;
            }
        }
    }
}
