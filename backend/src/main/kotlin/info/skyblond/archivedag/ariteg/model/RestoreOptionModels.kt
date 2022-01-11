package info.skyblond.archivedag.ariteg.model

import software.amazon.awssdk.services.s3.model.Tier

/**
 * Abstract interface for restore protos
 * */
data class RestoreOption(
    val days: Int? = null,
    val tier: Tier? = null
)

// TODO AWS S3 Restore option
