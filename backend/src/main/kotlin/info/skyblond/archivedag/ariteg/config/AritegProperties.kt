package info.skyblond.archivedag.ariteg.config

import io.ipfs.multihash.Multihash
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import java.util.concurrent.TimeUnit

@ConstructorBinding
@ConfigurationProperties(prefix = "archive-dag.ariteg")
data class AritegProperties(
    val meta: ProtoMetaProperties,
    val storage: ProtoStorageProperties,
) {
    data class ProtoMetaProperties(
        val lockExpireDuration: Long = 5,
        val lockExpireTimeUnit: TimeUnit = TimeUnit.MINUTES
    )

    data class ProtoStorageProperties(
        // common settings
        val type: ProtoRepoType,
        val primaryHashType: Multihash.Type,
        val secondaryHashType: Multihash.Type,
        // file system settings
        val filesystem: FileSystemProperties,
    ) {
        enum class ProtoRepoType {
            FILE_SYSTEM, AWS_S3,  // TODO ?
            HYBRID // TODO S3 for storage, Local FS as cache
        }

        data class FileSystemProperties(
            val path: String,
            val queueSize: Int = Int.MAX_VALUE,
            val threadSize: Int = Runtime.getRuntime().availableProcessors()
        )
    }
}
