package info.skyblond.archivedag.ariteg.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import software.amazon.awssdk.services.s3.model.StorageClass

@ConstructorBinding
@ConfigurationProperties(prefix = "ariteg")
data class AritegProperties(
    val storage: ProtoStorageProperties,
) {
    data class ProtoStorageProperties(
        // common settings
        val type: ProtoRepoType = ProtoRepoType.LOCAL_FILE_SYSTEM_ONLY,
        val queueSize: Int = Int.MAX_VALUE,
        val threadSize: Int = Runtime.getRuntime().availableProcessors(),
        val filesystem: FileSystemProperties?,
        val s3: S3Properties?
    ) {
        enum class ProtoRepoType {
            LOCAL_FILE_SYSTEM_ONLY, // store proto only in local file system
            LOCAL_WITH_S3_ARCHIVE, // use file system, but upload to S3 for passive backup
        }

        data class FileSystemProperties(
            val path: String
        )

        data class S3Properties(
            val endpoint: String? = null,
            val accessKey: String? = null,
            val secretKey: String? = null,
            val region: String,
            val bucketName: String,
            val uploadStorageClass: StorageClass = StorageClass.DEEP_ARCHIVE,
        )
    }
}
