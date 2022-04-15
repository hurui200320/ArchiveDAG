package info.skyblond.archivedag.arudaz.model

import info.skyblond.archivedag.ariteg.AritegService
import info.skyblond.archivedag.ariteg.model.AritegObjects
import info.skyblond.archivedag.ariteg.protos.AritegLink
import info.skyblond.archivedag.ariteg.protos.AritegObjectType
import info.skyblond.archivedag.commons.PermissionDeniedException
import io.ipfs.multihash.Multihash
import java.util.*

data class TransferReceipt(
    val recordId: UUID,
    val username: String,
    val primaryHash: Multihash,
    val objectType: AritegObjectType
) {
    fun checkUsage(username: String, recordId: UUID, aritegService: AritegService): AritegLink {
        if (this.username != username) {
            // receipt uploader doesn't match current user
            throw PermissionDeniedException("You can only use your own receipt")
        }
        // check receipt uuid
        if (this.recordId != recordId) {
            // receipt record uuid doesn't match current record
            throw PermissionDeniedException("The receipt cannot be used in this file record")
        }
        // make sure the proto is existing
        require(aritegService.multihashExists(this.primaryHash)) { "The receipt is expired" }
        return AritegObjects.newLink(this.primaryHash, this.objectType)
    }
}
