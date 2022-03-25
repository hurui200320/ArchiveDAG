package info.skyblond.archivedag.actohw;

import java.io.File;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public interface Slicer {
    List<BlobDescriptor> digest(File file);

    Stream<CompletableFuture<BlobDescriptor>> digestAsync(File file);
}
