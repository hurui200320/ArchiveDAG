package info.skyblond.archivedag.config;


import info.skyblond.ariteg.chunking.ChunkProviderFactory;
import info.skyblond.ariteg.chunking.FixedLengthChunkProvider;
import info.skyblond.ariteg.multihash.MultihashProvider;
import info.skyblond.ariteg.multihash.MultihashProviders;
import info.skyblond.ariteg.service.impl.AritegFileStorageService;
import info.skyblond.ariteg.service.intf.AritegStorageService;
import kotlin.NotImplementedError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.util.Objects;

@Configuration
@EnableConfigurationProperties(ProtoProperties.class)
public class ProtoConfiguration {
    private final Logger logger = LoggerFactory.getLogger(ProtoConfiguration.class);
    private final ProtoProperties properties;

    public ProtoConfiguration(ProtoProperties properties) {
        this.properties = properties;
    }

    @Bean
    public ChunkProviderFactory resolveChunkProvider() {
        var type = this.properties.getChunkProvider();
        switch (type) {
            case FIXED_LENGTH:
                return inputStream -> new FixedLengthChunkProvider(inputStream, properties.getFixedBlobSize());
            case RABIN_FINGERPRINT:
                throw new NotImplementedError("TODO RABIN_FINGERPRINT");
            case FAST_CDC:
                throw new NotImplementedError("TODO FAST_CDC");
        }
        throw new IllegalArgumentException("Unknown type: " + type);
    }


    @Bean
    public AritegStorageService resolveStorage() {
        var storageProperties = this.properties.getStorage();
        MultihashProvider primary = MultihashProviders.fromMultihashType(
                Objects.requireNonNull(storageProperties.getPrimaryHashType())
        );
        MultihashProvider secondary = MultihashProviders.fromMultihashType(
                Objects.requireNonNull(storageProperties.getSecondaryHashType())
        );
        switch (Objects.requireNonNull(storageProperties.getType())) {
            case FILE_SYSTEM:
                return this.resolveFileSystem(primary, secondary);
            case AWS_S3:
                throw new NotImplementedError("TODO AWS_S3");
            case HYBRID:
                throw new NotImplementedError("TODO HYBRID");
        }
        throw new IllegalArgumentException("Unknown type: " + storageProperties.getType().name());
    }

    private AritegStorageService resolveFileSystem(MultihashProvider primary, MultihashProvider secondary) {
        this.logger.info("Using file system proto storage");
        var p = Objects.requireNonNull(this.properties.getStorage().getFilesystem());
        File baseDir = new File(Objects.requireNonNull(p.getPath()));
        if (baseDir.mkdirs()) {
            this.logger.trace("Create dir: {}", baseDir.getAbsolutePath());
        }
        return new AritegFileStorageService(
                primary, secondary, baseDir,
                p.getThreadSize(), p.getQueueSize()
        );
    }

}
