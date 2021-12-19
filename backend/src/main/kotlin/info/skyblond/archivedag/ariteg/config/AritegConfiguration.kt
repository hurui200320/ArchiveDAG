package info.skyblond.archivedag.ariteg.config

import info.skyblond.archivedag.ariteg.config.AritegProperties.ProtoStorageProperties.ProtoRepoType.FILE_SYSTEM
import info.skyblond.archivedag.ariteg.multihash.MultihashProvider
import info.skyblond.archivedag.ariteg.multihash.MultihashProviders.fromMultihashType
import info.skyblond.archivedag.ariteg.storage.AritegFileStorageService
import info.skyblond.archivedag.ariteg.storage.AritegStorageService
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.io.File

@Configuration
@EnableConfigurationProperties(AritegProperties::class)
class AritegConfiguration(
    private val properties: AritegProperties,
) {
    private val logger = LoggerFactory.getLogger(AritegConfiguration::class.java)

    @Bean
    fun resolveStorage(): AritegStorageService {
        val storageProperties = properties.storage
        val primary = fromMultihashType(storageProperties.primaryHashType)
        val secondary = fromMultihashType(storageProperties.secondaryHashType)
        return when (storageProperties.type) {
            FILE_SYSTEM -> resolveFileSystem(primary, secondary)
            else -> throw NotImplementedError("TODO")
        }
    }

    private fun resolveFileSystem(
        primary: MultihashProvider,
        secondary: MultihashProvider
    ): AritegStorageService {
        logger.info("Using file system proto storage")
        val p = properties.storage.filesystem
        val baseDir = File(p.path)
        if (baseDir.mkdirs()) {
            logger.trace("Create dir: {}", baseDir.absolutePath)
        }
        return AritegFileStorageService(
            primaryProvider = primary,
            secondaryProvider = secondary,
            baseDir = baseDir,
            threadNum = p.threadSize,
            queueSize = p.queueSize
        )
    }
}
