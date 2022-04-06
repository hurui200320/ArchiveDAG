package info.skyblond.archivedag.ariteg.config

import info.skyblond.archivedag.ariteg.config.AritegProperties.ProtoStorageProperties.ProtoRepoType.LOCAL_FILE_SYSTEM_ONLY
import info.skyblond.archivedag.ariteg.config.AritegProperties.ProtoStorageProperties.ProtoRepoType.LOCAL_WITH_S3_BACKUP
import info.skyblond.archivedag.ariteg.storage.AritegFileStorageService
import info.skyblond.archivedag.ariteg.storage.AritegS3ArchiveStorageService
import info.skyblond.archivedag.ariteg.storage.AritegStorageService
import info.skyblond.archivedag.commons.service.EtcdConfigService
import io.ipfs.multihash.Multihash
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Lazy
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import java.io.File
import java.net.URI

/**
 * This class config the proto things.
 * It will read config from etcd and local yml file,
 * then initialize [AritegStorageService] and an optional
 * [S3Client] object.
 * */
@Configuration
@EnableConfigurationProperties(AritegProperties::class)
class AritegConfiguration(
    private val properties: AritegProperties,
    private val etcdConfig: EtcdConfigService
) {
    private val etcdNamespace = "ariteg/proto"
    private val logger = LoggerFactory.getLogger(AritegConfiguration::class.java)

    fun getPrimaryHashType(): Multihash.Type {
        val configKey = "primary_hash_type"
        val primary = etcdConfig.requireString(etcdNamespace, configKey)
        logger.info("Primary hash type from config: $primary")
        return Multihash.Type.valueOf(primary)
    }

    fun getSecondaryHashType(): Multihash.Type {
        val configKey = "secondary_hash_type"
        val secondary = etcdConfig.requireString(etcdNamespace, configKey)
        logger.info("Secondary hash type from config: $secondary")
        return Multihash.Type.valueOf(secondary)
    }

    @Bean
    fun resolveStorage(): AritegStorageService {
        val storageProperties = properties.storage

        val primaryType = getPrimaryHashType()
        val secondaryType = getSecondaryHashType()

        return when (storageProperties.type) {
            LOCAL_FILE_SYSTEM_ONLY -> resolveFileSystem(primaryType, secondaryType)
            LOCAL_WITH_S3_BACKUP -> resolveS3(primaryType, secondaryType)
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
        val s3Properties = properties.storage.s3!!
        return S3Client.builder()
            .apply {
                s3Properties.endpoint?.let {
                    endpointOverride(URI.create(it))
                }
            }
            .region(Region.of(s3Properties.region))
            .apply {
                if (s3Properties.accessKey != null && s3Properties.secretKey != null) {
                    credentialsProvider {
                        AwsBasicCredentials.create(
                            properties.storage.s3.accessKey,
                            properties.storage.s3.secretKey
                        )
                    }
                }
            }
            .build()
    }
}
