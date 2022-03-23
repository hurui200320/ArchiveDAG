package info.skyblond.archivedag.actohw;

import java.io.File;
import java.util.List;

public interface Slicer {
    List<BlobDescriptor> digest(File file);
}
