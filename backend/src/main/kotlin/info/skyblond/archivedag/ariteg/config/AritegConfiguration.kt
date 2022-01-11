package info.skyblond.archivedag.ariteg.config

import info.skyblond.archivedag.ariteg.config.AritegProperties.ProtoStorageProperties.ProtoRepoType.LOCAL_FILE_SYSTEM_ONLY
import info.skyblond.archivedag.ariteg.config.AritegProperties.ProtoStorageProperties.ProtoRepoType.LOCAL_WITH_S3_ARCHIVE
import info.skyblond.archivedag.ariteg.storage.AritegFileStorageService
import info.skyblond.archivedag.ariteg.storage.AritegS3ArchiveStorageService
import info.skyblond.archivedag.ariteg.storage.AritegStorageService
import io.ipfs.multihash.Multihash
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Lazy
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
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
        val primary = storageProperties.primaryHashType
        val secondary = storageProperties.secondaryHashType
        return when (storageProperties.type) {
            LOCAL_FILE_SYSTEM_ONLY -> resolveFileSystem(primary, secondary)
            LOCAL_WITH_S3_ARCHIVE -> resolveS3(primary, secondary)
//            else -> throw NotImplementedError("TODO")
        }
    }

    private fun resolveS3(
        primary: Multihash.Type,
        secondary: Multihash.Type
    ): AritegStorageService {
        logger.info("Using AWS S3 proto storage")
        val p = properties.storage
        val baseDir = File(p.filesystem!!.path)
        if (baseDir.mkdirs()) {
            logger.trace("Create dir: {}", baseDir.absolutePath)
        }
        val client = s3Client()
        return AritegS3ArchiveStorageService(
            primaryProviderType = primary,
            secondaryProviderType = secondary,
            baseDir = baseDir,
            s3Client = client,
            bucketName = p.s3!!.bucketName,
            uploadStorageClass = p.s3.uploadStorageClass,
            threadNum = p.threadSize,
            queueSize = p.queueSize
        )
    }

    private fun resolveFileSystem(
        primary: Multihash.Type,
        secondary: Multihash.Type
    ): AritegStorageService {
        logger.info("Using file system proto storage")
        val p = properties.storage
        val baseDir = File(p.filesystem!!.path)
        if (baseDir.mkdirs()) {
            logger.trace("Create dir: {}", baseDir.absolutePath)
        }
        return AritegFileStorageService(
            primaryProviderType = primary,
            secondaryProviderType = secondary,
            baseDir = baseDir,
            threadNum = p.threadSize,
            queueSize = p.queueSize
        )
    }

    @Lazy
    @Bean
    fun s3Client(): S3Client {
        return S3Client.builder().region(Region.of(properties.storage.s3!!.region)).build()
    }
}
