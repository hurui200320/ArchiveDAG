package info.skyblond.archivedag.arudaz.controller.grpc

import com.google.protobuf.ByteString
import info.skyblond.archivedag.ariteg.AritegService
import info.skyblond.archivedag.ariteg.model.*
import info.skyblond.archivedag.ariteg.multihash.MultihashProviders
import info.skyblond.archivedag.ariteg.protos.AritegLink
import info.skyblond.archivedag.ariteg.protos.AritegListObject
import info.skyblond.archivedag.ariteg.protos.AritegTreeObject
import info.skyblond.archivedag.ariteg.utils.toMultihash
import info.skyblond.archivedag.arstue.FileRecordService
import info.skyblond.archivedag.arstue.FileRecordService.Companion.READ_PERMISSION_BIT
import info.skyblond.archivedag.arstue.FileRecordService.Companion.UPDATE_REF_PERMISSION_BIT
import info.skyblond.archivedag.arstue.GroupService
import info.skyblond.archivedag.arudaz.model.TransferReceipt
import info.skyblond.archivedag.arudaz.protos.common.Empty
import info.skyblond.archivedag.arudaz.protos.transfer.*
import info.skyblond.archivedag.arudaz.service.TransferReceiptService
import info.skyblond.archivedag.arudaz.utils.getCurrentUsername
import info.skyblond.archivedag.commons.EntityNotFoundException
import info.skyblond.archivedag.commons.PermissionDeniedException
import io.grpc.stub.StreamObserver
import io.ipfs.multihash.Multihash
import net.devh.boot.grpc.server.service.GrpcService
import org.springframework.data.domain.Pageable
import org.springframework.security.access.prepost.PreAuthorize
import java.util.*

@GrpcService
@PreAuthorize("@applicationConfigService.allowGrpcWriteProto()")
class TransferController(
    private val fileRecordService: FileRecordService,
    private val transferReceiptService: TransferReceiptService,
    private val aritegService: AritegService,
    private val groupService: GroupService
) : ProtoTransferServiceGrpc.ProtoTransferServiceImplBase() {

    @PreAuthorize("hasAnyRole('VIEWER')")
    override fun queryServerProtoConfig(
        request: Empty,
        responseObserver: StreamObserver<QueryServerProtoConfigResponse>
    ) {
        responseObserver.onNext(
            QueryServerProtoConfigResponse.newBuilder()
                .setPrimaryHashType(aritegService.primaryMultihashType().name)
                .setSecondaryHashType(aritegService.secondaryMultihashType().name)
                .build()
        )
        responseObserver.onCompleted()
    }


    private fun queryPermission(username: String, recordUUID: UUID): Int {
        val groups = groupService.listUserOwnedGroup(
            username, Pageable.unpaged()
        ) + groupService.listUserJoinedGroup(
            username, Pageable.unpaged()
        )
        return fileRecordService.queryPermission(recordUUID, username, groups)
    }

    private fun preUploadCheck(recordUUID: UUID, primaryHash: Multihash, content: ByteArray) {
        val username = getCurrentUsername()
        if (queryPermission(username, recordUUID) and UPDATE_REF_PERMISSION_BIT == 0) {
            // no update ref permission -> cannot upload data
            throw PermissionDeniedException("You cannot upload data for this record")
        }
        // check data
        MultihashProviders.mustMatch(primaryHash, content)
    }

    private fun handleUploadProcess(
        recordUUID: UUID,
        aritegObject: AritegObject,
        responseObserver: StreamObserver<TransferReceiptResponse>
    ) {
        val username = getCurrentUsername()
        // write into system
        val writeReceipt = aritegService.writeProto("", aritegObject)
        // calculate transfer receipt
        val transferReceipt = transferReceiptService.encryptReceipt(
            TransferReceipt(recordUUID, username, writeReceipt.link.multihash.toMultihash())
        )
        val result = TransferReceiptResponse.newBuilder()
            .setPrimaryHash(writeReceipt.link.multihash)
            .setTransferReceipt(transferReceipt)
            .build()
        // wait writing
        writeReceipt.completionFuture.get()
        // return the result
        responseObserver.onNext(result)
        responseObserver.onCompleted()
    }

    @PreAuthorize("hasRole('UPLOADER')")
    override fun uploadBlob(
        request: UploadBlobRequest,
        responseObserver: StreamObserver<TransferReceiptResponse>
    ) {
        val uuid = UUID.fromString(request.recordUuid)
        val primaryHash = request.primaryHash.toMultihash()
        preUploadCheck(uuid, primaryHash, request.blobObj.toByteArray())
        // write into system
        handleUploadProcess(uuid, BlobObject.fromProto(request.blobObj), responseObserver)
    }

    @PreAuthorize("hasRole('UPLOADER')")
    override fun uploadList(request: UploadListRequest, responseObserver: StreamObserver<TransferReceiptResponse>) {
        val uuid = UUID.fromString(request.recordUuid)
        val primaryHash = request.primaryHash.toMultihash()
        preUploadCheck(uuid, primaryHash, request.listObj.toByteArray())
        // decrypt receipt
        val username = getCurrentUsername()
        val listObj = ListObject(request.listObj.linksList.map {
            val receipt = transferReceiptService.decryptReceipt(it.multihash.toStringUtf8())
                ?: throw IllegalArgumentException("Invalid list element: $it")
            require(receipt.recordId == uuid) { "Invalid uuid in element: $it" }
            require(receipt.username == username) { "Invalid username in element: $it" }
            AritegObjects.newLink(receipt.primaryHash, it.type)
        })
        // write into system
        handleUploadProcess(uuid, listObj, responseObserver)
    }

    @PreAuthorize("hasRole('UPLOADER')")
    override fun uploadTree(request: UploadTreeRequest, responseObserver: StreamObserver<TransferReceiptResponse>) {
        val uuid = UUID.fromString(request.recordUuid)
        val primaryHash = request.primaryHash.toMultihash()
        preUploadCheck(uuid, primaryHash, request.treeObj.toByteArray())
        // decrypt receipt
        val username = getCurrentUsername()
        val treeObj = TreeObject(request.treeObj.linksList.map {
            val receipt = transferReceiptService.decryptReceipt(it.multihash.toStringUtf8())
                ?: throw IllegalArgumentException("Invalid list element: $it")
            require(receipt.recordId == uuid) { "Invalid uuid in element: $it" }
            require(receipt.username == username) { "Invalid username in element: $it" }
            AritegObjects.newLink(it.name, receipt.primaryHash, it.type)
        })
        // write into system
        handleUploadProcess(uuid, treeObj, responseObserver)
    }

    @PreAuthorize("hasAnyRole('VIEWER')")
    override fun proveOwnership(
        request: ProveOwnershipRequest,
        responseObserver: StreamObserver<TransferReceiptResponse>
    ) {
        val username = getCurrentUsername()
        val uuid = UUID.fromString(request.recordUuid)
        if (queryPermission(username, uuid) and UPDATE_REF_PERMISSION_BIT == 0) {
            // no update ref permission -> cannot upload data
            throw PermissionDeniedException("You cannot upload data for this record")
        }
        val primaryHash = request.primaryHash.toMultihash()
        val secondaryHash = request.secondaryHash.toMultihash()
        // Return true if primary collided, return false if not.
        // Return null if not found
        val collision = aritegService.checkCollision(primaryHash, secondaryHash)
            ?: throw EntityNotFoundException("Proto object not found")
        if (collision) {
            throw IllegalStateException("Hash collision detected!")
        }
        // everything is ok
        val transferReceipt = transferReceiptService.encryptReceipt(
            TransferReceipt(uuid, username, primaryHash)
        )
        val result = TransferReceiptResponse.newBuilder()
            .setPrimaryHash(request.primaryHash)
            .setTransferReceipt(transferReceipt)
            .build()
        // return the result
        responseObserver.onNext(result)
        responseObserver.onCompleted()
    }

    private fun preDownloadCheck(transferReceiptString: String): Pair<TransferReceipt, AritegLink> {
        val transferReceipt = transferReceiptService.decryptReceipt(transferReceiptString)
            ?: throw IllegalArgumentException("Invalid transfer receipt")
        val username = getCurrentUsername()
        val aritegLink = transferReceipt.checkUsage(username, transferReceipt.recordId, aritegService)
        if (queryPermission(username, transferReceipt.recordId) and READ_PERMISSION_BIT == 0) {
            // no read permission -> cannot read data
            throw PermissionDeniedException("You cannot read this record")
        }
        return transferReceipt to aritegLink
    }

    @PreAuthorize("hasAnyRole('VIEWER')")
    override fun readBlob(request: ReadObjectRequest, responseObserver: StreamObserver<ReadBlobResponse>) {
        val (_, aritegLink) = preDownloadCheck(request.transferReceipt)
        // pass the check, read the proto
        val blob = aritegService.readBlob(aritegLink)
        // send it back
        responseObserver.onNext(
            ReadBlobResponse.newBuilder()
                .setPrimaryHash(aritegLink.multihash)
                .setBlobObj(blob.toProto())
                .build()
        )
        responseObserver.onCompleted()
    }

    @PreAuthorize("hasAnyRole('VIEWER')")
    override fun readList(request: ReadObjectRequest, responseObserver: StreamObserver<ReadListResponse>) {
        val (transferReceipt, aritegLink) = preDownloadCheck(request.transferReceipt)
        val username = getCurrentUsername()
        // pass the check, read the list
        val rawListObj = aritegService.readList(aritegLink)
        // map multihash to receipt
        val encryptedLinkList = rawListObj.list.map {
            val receipt = transferReceiptService.encryptReceipt(
                TransferReceipt(transferReceipt.recordId, username, it.multihash.toMultihash())
            )
            AritegLink.newBuilder()
                .setType(it.type)
                .setMultihash(ByteString.copyFrom(receipt, Charsets.UTF_8))
                .build()
        }
        // send it back
        responseObserver.onNext(
            ReadListResponse.newBuilder()
                .setPrimaryHash(aritegLink.multihash)
                .setListObj(
                    AritegListObject.newBuilder()
                        .addAllLinks(encryptedLinkList)
                        .build()
                )
                .build()
        )
        responseObserver.onCompleted()
    }

    @PreAuthorize("hasAnyRole('VIEWER')")
    override fun readTree(request: ReadObjectRequest, responseObserver: StreamObserver<ReadTreeResponse>) {
        val (transferReceipt, aritegLink) = preDownloadCheck(request.transferReceipt)
        val username = getCurrentUsername()
        // pass the check, read the tree
        val rawTreeObj = aritegService.readTree(aritegLink)
        // map multihash to receipt
        val encryptedLinkList = rawTreeObj.links.map {
            val receipt = transferReceiptService.encryptReceipt(
                TransferReceipt(transferReceipt.recordId, username, it.multihash.toMultihash())
            )
            AritegLink.newBuilder()
                .setName(it.name)
                .setType(it.type)
                .setMultihash(ByteString.copyFrom(receipt, Charsets.UTF_8))
                .build()
        }
        // send it back
        responseObserver.onNext(
            ReadTreeResponse.newBuilder()
                .setPrimaryHash(aritegLink.multihash)
                .setTreeObj(
                    AritegTreeObject.newBuilder()
                        .addAllLinks(encryptedLinkList)
                        .build()
                )
                .build()
        )
        responseObserver.onCompleted()
    }

    @PreAuthorize("hasAnyRole('VIEWER')")
    override fun readCommit(request: ReadObjectRequest, responseObserver: StreamObserver<ReadCommitResponse>) {
        val (transferReceipt, aritegLink) = preDownloadCheck(request.transferReceipt)
        val username = getCurrentUsername()
        // pass the check, read the commit
        val rawCommitObj = aritegService.readCommit(aritegLink)
        // map multihash to receipt
        val encryptedParentLink = rawCommitObj.parentLink.let {
            if (it.multihash.isEmpty) {
                it // initial commit, empty link
            } else {
                val receipt = transferReceiptService.encryptReceipt(
                    TransferReceipt(transferReceipt.recordId, username, it.multihash.toMultihash())
                )
                AritegLink.newBuilder()
                    .setType(it.type)
                    .setMultihash(ByteString.copyFrom(receipt, Charsets.UTF_8))
                    .build()
            }
        }
        val encryptedCommitLink = rawCommitObj.committedObjectLink.let {
            val receipt = transferReceiptService.encryptReceipt(
                TransferReceipt(transferReceipt.recordId, username, it.multihash.toMultihash())
            )
            AritegLink.newBuilder()
                .setType(it.type)
                .setMultihash(ByteString.copyFrom(receipt, Charsets.UTF_8))
                .build()
        }
        val encryptedAuthorLink = rawCommitObj.authorLink.let {
            val receipt = transferReceiptService.encryptReceipt(
                TransferReceipt(transferReceipt.recordId, username, it.multihash.toMultihash())
            )
            AritegLink.newBuilder()
                .setType(it.type)
                .setMultihash(ByteString.copyFrom(receipt, Charsets.UTF_8))
                .build()
        }
        // rebuild result obj
        val resultObj = rawCommitObj.toProto().toBuilder()
            .setParent(encryptedParentLink)
            .setCommittedObject(encryptedCommitLink)
            .setAuthor(encryptedAuthorLink)
            .build()
        // send it back
        responseObserver.onNext(
            ReadCommitResponse.newBuilder()
                .setPrimaryHash(aritegLink.multihash)
                .setCommitObj(resultObj)
                .build()
        )
        responseObserver.onCompleted()
    }

    @PreAuthorize("hasAnyRole('VIEWER')")
    override fun validateObject(
        request: ReadObjectRequest,
        responseObserver: StreamObserver<ValidateObjectResponse>
    ) {
        val (_, aritegLink) = preDownloadCheck(request.transferReceipt)
        // send it back
        responseObserver.onNext(
            ValidateObjectResponse.newBuilder()
                .setPrimaryHash(aritegLink.multihash)
                .build()
        )
        responseObserver.onCompleted()
    }

    @PreAuthorize("hasAnyRole('VIEWER')")
    override fun cherryPick(request: CherryPickRequest, responseObserver: StreamObserver<TransferReceiptResponse>) {
        val (_, aritegLink) = preDownloadCheck(request.currentTransferReceipt)
        val username = getCurrentUsername()
        val newUUID = UUID.fromString(request.targetRecordUuid)
        if (queryPermission(username, newUUID) and UPDATE_REF_PERMISSION_BIT == 0) {
            // no update ref permission -> cannot upload data
            throw PermissionDeniedException("You cannot upload data for this record")
        }
        // sign new receipt
        val newTransferReceipt = transferReceiptService.encryptReceipt(
            TransferReceipt(newUUID, username, aritegLink.multihash.toMultihash())
        )
        // send it back
        responseObserver.onNext(
            TransferReceiptResponse.newBuilder()
                .setPrimaryHash(aritegLink.multihash)
                .setTransferReceipt(newTransferReceipt)
                .build()
        )
        responseObserver.onCompleted()
    }
}
