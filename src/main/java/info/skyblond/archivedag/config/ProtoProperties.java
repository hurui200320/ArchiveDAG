package info.skyblond.archivedag.config;

import io.ipfs.multihash.Multihash;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "archive-dag.proto")
@Getter
@Setter
public class ProtoProperties {
    private ProtoMetaProperties meta = new ProtoMetaProperties();
    private ProtoStorageProperties storage = new ProtoStorageProperties();

    // Default blob size 16MB
    private int blobSize = 16 * 1024 * 1024;
    // single list can represent 64 GB file
    private int listLength = 4096;

    @Getter
    @Setter
    static class ProtoMetaProperties {
        private long lockExpireTimeInMs = 300_000;
    }

    @Getter
    @Setter
    static class ProtoStorageProperties {
        // common settings
        private ProtoRepoType type;
        private Multihash.Type primaryHashType;
        private Multihash.Type secondaryHashType;

        enum ProtoRepoType {
            FILE_SYSTEM,
            AWS_S3, // TODO ?
            HYBRID // TODO S3 for storage, Local FS as cache
        }

        // file system settings
        private FileSystemProperties filesystem;

        @Getter
        @Setter
        static class FileSystemProperties {
            private String path;
            private int queueSize;
            private int threadSize;
        }
    }
}
