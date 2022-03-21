package info.skyblond.archivedag.actohw;

import java.io.InputStream;
import java.util.List;

public interface Slicer {
    List<BlobDescriptor> digest(InputStream input);
}
