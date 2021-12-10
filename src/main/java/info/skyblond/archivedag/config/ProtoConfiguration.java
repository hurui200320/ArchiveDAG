package info.skyblond.archivedag.config;


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
                return this.resolveAwsS3(primary, secondary);
            case HYBRID:
                return this.resolveHybrid(primary, secondary);
        }
        throw new IllegalArgumentException("Unknown type: " + storageProperties.getType().name());
    }

    private AritegStorageService resolveHybrid(MultihashProvider primary, MultihashProvider secondary) {
        // TODO
        throw new NotImplementedError("TODO");
    }

    private AritegStorageService resolveAwsS3(MultihashProvider primary, MultihashProvider secondary) {
        // TODO
        throw new NotImplementedError("TODO");
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
