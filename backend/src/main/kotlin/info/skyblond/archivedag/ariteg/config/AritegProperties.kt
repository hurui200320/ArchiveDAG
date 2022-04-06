package info.skyblond.archivedag.ariteg.config

import info.skyblond.archivedag.ariteg.config.AritegProperties.ProtoStorageProperties
import info.skyblond.archivedag.ariteg.config.AritegProperties.ProtoStorageProperties.*
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import software.amazon.awssdk.services.s3.model.StorageClass

/**
 * Config definition for string yml config.
 * */
@ConstructorBinding
@ConfigurationProperties(prefix = "ariteg")
data class AritegProperties(
    /**
     * The storage configs. See [ProtoStorageProperties]
     * */
    val storage: ProtoStorageProperties,
) {
    data class ProtoStorageProperties(
        /**
         * Configure the storage. See [ProtoRepoType]
         * */
        val type: ProtoRepoType = ProtoRepoType.LOCAL_FILE_SYSTEM_ONLY,
        /**
         * Queue size for writing request handling thread pool.
         * Too small will impact the performance.
         * Too big will use too much memory.
         * */
        val queueSize: Int = Int.MAX_VALUE,
        /**
         * The thread pool size, aka how much worker threads.
         * */
        val threadSize: Int = Runtime.getRuntime().availableProcessors(),
        /**
         * Config local file system. See [FileSystemProperties]
         * */
        val filesystem: FileSystemProperties?,
        /**
         * Config s3 client. See [S3Properties]
         * */
        val s3: S3Properties?
    ) {
        enum class ProtoRepoType {
            /**
             * Store all protos on local file system.
             * It's better to be a iSCSI device or something with backup.
             * */
            LOCAL_FILE_SYSTEM_ONLY,

            /**
             * Store all protos on local file system and target s3 bucket.
             * When writing, write both location.
             * When reading, use local copy, if missing, download from s3.
             * */
            LOCAL_WITH_S3_BACKUP,
        }

        data class FileSystemProperties(
            /**
             * Root path to store protos.
             * */
            val path: String
        )

        data class S3Properties(
            /**
             * Endpoint url for target s3 service.
             * Default is aws s3.
             * */
            val endpoint: String? = null,
            /**
             * Access key for the api.
             * Default reads from s3 sdk.
             * */
            val accessKey: String? = null,
            /**
             * Secret key for the api.
             * Default reads from s3 sdk.
             * */
            val secretKey: String? = null,
            /**
             * Bucket region.
             * */
            val region: String,
            /**
             * Bucket name
             * */
            val bucketName: String,
            /**
             * Which storage class is uploaded to.
             * Default: standard.
             * */
            val uploadStorageClass: StorageClass = StorageClass.STANDARD,
        )
    }
}
