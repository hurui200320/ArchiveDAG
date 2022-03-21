package info.skyblond.archivedag.arudaz.controller.grpc

import com.google.protobuf.ByteString
import info.skyblond.archivedag.ariteg.AritegService
import info.skyblond.archivedag.ariteg.model.BlobObject
import info.skyblond.archivedag.ariteg.model.CommitObject
import info.skyblond.archivedag.ariteg.protos.AritegLink
import info.skyblond.archivedag.ariteg.protos.AritegObjectType
import info.skyblond.archivedag.ariteg.utils.toMultihash
import info.skyblond.archivedag.arstue.FileRecordService
import info.skyblond.archivedag.arstue.GroupService
import info.skyblond.archivedag.arstue.UserManagementService
import info.skyblond.archivedag.arstue.entity.RecordAccessControlEntity
import info.skyblond.archivedag.arudaz.model.TransferReceipt
import info.skyblond.archivedag.arudaz.protos.common.Empty
import info.skyblond.archivedag.arudaz.protos.common.Page
import info.skyblond.archivedag.arudaz.protos.record.*
import info.skyblond.archivedag.arudaz.service.TransferReceiptService
import info.skyblond.archivedag.arudaz.utils.getCurrentUsername
import info.skyblond.archivedag.arudaz.utils.parsePagination
import info.skyblond.archivedag.commons.PermissionDeniedException
import info.skyblond.archivedag.commons.getUnixTimestamp
import io.grpc.stub.StreamObserver
import net.devh.boot.grpc.server.service.GrpcService
import org.springframework.data.domain.Pageable
import org.springframework.security.access.prepost.PreAuthorize
import java.util.*

@GrpcService
class FileRecordController(
    private val fileRecordService: FileRecordService,
    private val transferReceiptService: TransferReceiptService,
    private val aritegService: AritegService,
    private val groupService: GroupService,
    private val userManagementService: UserManagementService
) : FileRecordServiceGrpc.FileRecordServiceImplBase() {

    @PreAuthorize("hasAnyRole('UPLOADER') && @applicationConfigService.allowGrpcWriteProto()")
    override fun createFileRecord(
        request: CreateFileRecordRequest,
        responseObserver: StreamObserver<FileRecordUuidListResponse>
    ) {
        val uuid = fileRecordService.createRecord(request.recordName, getCurrentUsername())
        responseObserver.onNext(FileRecordUuidListResponse.newBuilder().addRecordUuid(uuid.toString()).build())
        responseObserver.onCompleted()
    }

    @PreAuthorize("hasRole('UPLOADER') && @applicationConfigService.allowGrpcWriteProto()")
    override fun updateFileRecordRef(request: UpdateFileRecordRefRequest, responseObserver: StreamObserver<Empty>) {
        val username = getCurrentUsername()
        val recordUUID = UUID.fromString(request.recordUuid)
        val receipt = try {
            transferReceiptService.decryptReceipt(request.receipt)
        } catch (_: Exception) {
            null
        } ?: throw IllegalArgumentException("Invalid receipt")
        // check the receipt
        val targetLink = receipt.checkUsage(username, recordUUID, aritegService)
        // check his/her permission
        val groups = groupService.listUserJoinedGroup(username, Pageable.unpaged())
        val permission = fileRecordService.queryPermission(recordUUID, username, groups)
        if (permission and FileRecordService.UPDATE_REF_PERMISSION_BIT == 0) {
            // do not have update ref permission
            throw PermissionDeniedException("You can't perform update ref on this record")
        }
        // get old ref link, use empty link if null
        val oldRef = fileRecordService.queryRecord(recordUUID).multihash
            ?.let { aritegService.parseMultihash(it) } ?: AritegLink.newBuilder()
            .setType(AritegObjectType.COMMIT).build()

        // write author
        val authorReceipt = aritegService.writeProto(
            "author",
            BlobObject(ByteString.copyFrom(username, Charsets.UTF_8))
        ).also { it.completionFuture.get() }
        val commitObject = CommitObject(
            unixTimestamp = getUnixTimestamp(),
            message = request.message,
            parentLink = oldRef,
            committedObjectLink = targetLink, authorLink = authorReceipt.link
        )
        val commitReceipt = aritegService.writeProto("", commitObject)
        // wait writing data into system
        commitReceipt.completionFuture.get()
        // update ref
        fileRecordService.setRecordRef(recordUUID, commitReceipt.link.multihash.toMultihash())
        responseObserver.onNext(Empty.getDefaultInstance())
        responseObserver.onCompleted()
    }

    @PreAuthorize("hasRole('UPLOADER')")
    override fun updateFileRecordName(request: UpdateFileRecordNameRequest, responseObserver: StreamObserver<Empty>) {
        val username = getCurrentUsername()
        val recordUUID = UUID.fromString(request.recordUuid)
        // check his/her permission
        val groups = groupService.listUserJoinedGroup(username, Pageable.unpaged())
        val permission = fileRecordService.queryPermission(recordUUID, username, groups)
        if (permission and FileRecordService.UPDATE_NAME_PERMISSION_BIT == 0) {
            // do not have update ref permission
            throw PermissionDeniedException("You can't perform update name on this record")
        }
        // update name
        fileRecordService.setRecordName(recordUUID, request.newRecordName)
        responseObserver.onNext(Empty.getDefaultInstance())
        responseObserver.onCompleted()
    }

    @PreAuthorize("hasAnyRole('UPLOADER')")
    override fun transferFileRecord(request: TransferFileRecordRequest, responseObserver: StreamObserver<Empty>) {
        val username = getCurrentUsername()
        val recordUUID = UUID.fromString(request.recordUuid)
        // only owner can do this
        if (fileRecordService.queryRecord(recordUUID).owner != username) {
            throw PermissionDeniedException("You can't transfer this record")
        }
        // check new owner
        val newOwner = request.newOwner
        require(userManagementService.userExists(newOwner)) { "New owner doesn't exists" }
        // update owner
        fileRecordService.setRecordOwner(recordUUID, newOwner)
        responseObserver.onNext(Empty.getDefaultInstance())
        responseObserver.onCompleted()
    }

    @PreAuthorize("hasAnyRole('UPLOADER') && @applicationConfigService.allowGrpcWriteProto()")
    override fun deleteFileRecord(request: DeleteFileRecordRequest, responseObserver: StreamObserver<Empty>) {
        val username = getCurrentUsername()
        val recordUUID = UUID.fromString(request.recordUuid)
        // only owner can do this
        if (fileRecordService.queryRecord(recordUUID).owner != username) {
            throw PermissionDeniedException("You can't delete this record")
        }
        // delete record
        fileRecordService.deleteRecord(recordUUID)
        responseObserver.onNext(Empty.getDefaultInstance())
        responseObserver.onCompleted()
    }

    @PreAuthorize("hasAnyRole('VIEWER')")
    override fun queryFileRecord(
        request: QueryFileRecordRequest,
        responseObserver: StreamObserver<FileRecordDetailResponse>
    ) {
        val username = getCurrentUsername()
        val recordUUID = UUID.fromString(request.recordUuid)
        // check his/her permission
        val groups = groupService.listUserJoinedGroup(username, Pageable.unpaged())
        val permission = fileRecordService.queryPermission(recordUUID, username, groups)
        if (permission and FileRecordService.READ_PERMISSION_BIT == 0) {
            // do not have update ref permission
            throw PermissionDeniedException("You can't query this record")
        }
        // query
        val queryResult = fileRecordService.queryRecord(recordUUID)
        responseObserver.onNext(FileRecordDetailResponse.newBuilder()
            .setRecordUuid(queryResult.recordId.toString())
            .setRecordName(queryResult.recordName)
            .also {
                if (queryResult.multihash != null) {
                    it.receipt = transferReceiptService.encryptReceipt(
                        TransferReceipt(
                            queryResult.recordId, username, queryResult.multihash
                        )
                    )
                }
            }
            .setCreatedTimestamp(queryResult.createdTimestamp)
            .setOwner(queryResult.owner)
            .build())
        responseObserver.onCompleted()
    }

    @PreAuthorize("hasAnyRole('VIEWER')")
    override fun listOwnedFileRecords(
        request: Page,
        responseObserver: StreamObserver<FileRecordUuidListResponse>
    ) {
        val pageable: Pageable = parsePagination(request)
        val result = fileRecordService.listOwnedRecords(getCurrentUsername(), pageable)
        responseObserver.onNext(
            FileRecordUuidListResponse.newBuilder()
                .addAllRecordUuid(result.map { it.toString() })
                .build()
        )
        responseObserver.onCompleted()
    }

    @PreAuthorize("hasAnyRole('VIEWER')")
    override fun listUserSharedFileRecords(
        request: Page,
        responseObserver: StreamObserver<FileRecordUuidListResponse>
    ) {
        val pageable: Pageable = parsePagination(request)
        val result = fileRecordService.listUserSharedRecords(getCurrentUsername(), pageable)
        responseObserver.onNext(
            FileRecordUuidListResponse.newBuilder()
                .addAllRecordUuid(result.map { it.toString() })
                .build()
        )
        responseObserver.onCompleted()
    }

    @PreAuthorize("hasAnyRole('VIEWER')")
    override fun listGroupSharedFileRecords(
        request: ListGroupSharedFileRecordsRequest,
        responseObserver: StreamObserver<FileRecordUuidListResponse>
    ) {
        val username = getCurrentUsername()
        val groupName = request.groupName
        if (!groupService.userIsGroupOwner(groupName, username)
            && !groupService.userIsGroupMember(groupName, username)
        ) {
            // not owner nor member
            throw PermissionDeniedException("You cannot list shared file records for this group")
        }
        val pageable: Pageable = parsePagination(request.pagination)
        val result = fileRecordService.listGroupSharedRecords(groupName, pageable)
        responseObserver.onNext(
            FileRecordUuidListResponse.newBuilder()
                .addAllRecordUuid(result.map { it.toString() })
                .build()
        )
        responseObserver.onCompleted()
    }

    @PreAuthorize("hasAnyRole('VIEWER')")
    override fun listPublicSharedFileRecords(
        request: Page,
        responseObserver: StreamObserver<FileRecordUuidListResponse>
    ) {
        val pageable: Pageable = parsePagination(request)
        val result = fileRecordService.listPublicSharedRecords(pageable)
        responseObserver.onNext(
            FileRecordUuidListResponse.newBuilder()
                .addAllRecordUuid(result.map { it.toString() })
                .build()
        )
        responseObserver.onCompleted()
    }

    @PreAuthorize("hasRole('UPLOADER')")
    override fun setSharedRuleForRecord(request: SharedRule, responseObserver: StreamObserver<Empty>) {
        val recordUUID = UUID.fromString(request.recordUuid)
        val record = fileRecordService.queryRecord(recordUUID)
        if (record.owner != getCurrentUsername()) {
            // only owner can update access rules
            throw PermissionDeniedException("You cannot set shared rules on this record")
        }
        // check permission
        require(request.permission.isNotBlank()) { "Permission cannot be blank" }
        // check type and target
        val (requestType, requestTarget) = when (request.ruleType) {
            SharedRuleType.USER -> {
                // check user exists
                require(userManagementService.userExists(request.ruleTarget)) { "User doesn't exist" }
                RecordAccessControlEntity.Type.USER to request.ruleTarget
            }
            SharedRuleType.GROUP -> {
                // check group exists
                require(groupService.groupExists(request.ruleTarget)) { "Group doesn't exist" }
                RecordAccessControlEntity.Type.GROUP to request.ruleTarget
            }
            SharedRuleType.OTHER -> {
                RecordAccessControlEntity.Type.OTHER to ""
            }
            else -> throw IllegalArgumentException("Invalid type: ${request.ruleType}")
        }
        // update
        fileRecordService.setAccessRule(
            recordUUID, requestType, requestTarget, FileRecordService.permissionStringToInt(request.permission)
        )
        responseObserver.onNext(Empty.getDefaultInstance())
        responseObserver.onCompleted()
    }

    @PreAuthorize("hasRole('UPLOADER')")
    override fun deleteSharedRuleForRecord(
        request: DeleteSharedRuleForRecordRequest,
        responseObserver: StreamObserver<Empty>
    ) {
        val recordUUID = UUID.fromString(request.recordUuid)
        val record = fileRecordService.queryRecord(recordUUID)
        if (record.owner != getCurrentUsername()) {
            // only owner can update access rules
            throw PermissionDeniedException("You cannot delete shared rules on this record")
        }
        // check type and target
        val (requestType, requestTarget) = when (request.ruleType) {
            SharedRuleType.USER -> {
                RecordAccessControlEntity.Type.USER to request.ruleTarget
            }
            SharedRuleType.GROUP -> {
                RecordAccessControlEntity.Type.GROUP to request.ruleTarget
            }
            SharedRuleType.OTHER -> {
                RecordAccessControlEntity.Type.OTHER to ""
            }
            else -> throw IllegalArgumentException("Invalid type: ${request.ruleType}")
        }
        // delete
        fileRecordService.deleteAccessRule(recordUUID, requestType, requestTarget)
        responseObserver.onNext(Empty.getDefaultInstance())
        responseObserver.onCompleted()
    }

    @PreAuthorize("hasAnyRole('VIEWER')")
    override fun listSharedRulesForRecord(
        request: ListSharedRulesForRecordRequest,
        responseObserver: StreamObserver<SharedRuleListResponse>
    ) {
        val recordUUID = UUID.fromString(request.recordUuid)
        val record = fileRecordService.queryRecord(recordUUID)
        if (record.owner != getCurrentUsername()) {
            // only owner can update access rules
            throw PermissionDeniedException("You cannot list shared rules on this record")
        }
        // query/list
        val result = fileRecordService.listAccessRules(recordUUID, Pageable.unpaged())
        responseObserver.onNext(
            SharedRuleListResponse.newBuilder()
                .addAllSharedRule(result.map {
                    SharedRule.newBuilder()
                        .setRecordUuid(recordUUID.toString())
                        .setRuleType(
                            when (it.type) {
                                RecordAccessControlEntity.Type.USER -> SharedRuleType.USER
                                RecordAccessControlEntity.Type.GROUP -> SharedRuleType.GROUP
                                RecordAccessControlEntity.Type.OTHER -> SharedRuleType.OTHER
                            }
                        )
                        .setRuleTarget(it.target)
                        .setPermission(it.permission)
                        .build()
                })
                .build()
        )
        responseObserver.onCompleted()
    }

    @PreAuthorize("hasAnyRole('VIEWER')")
    override fun queryMyPermissionOnRecord(
        request: QueryMyPermissionOnRecordRequest,
        responseObserver: StreamObserver<SharedRulePermissionResponse>
    ) {
        val username = getCurrentUsername()
        val groups = groupService.listUserOwnedGroup(
            username, Pageable.unpaged()
        ) + groupService.listUserJoinedGroup(
            username, Pageable.unpaged()
        )
        val recordUUID = UUID.fromString(request.recordUuid)
        val permissionInt = fileRecordService.queryPermission(recordUUID, username, groups)

        responseObserver.onNext(
            SharedRulePermissionResponse.newBuilder()
                .setPermission(FileRecordService.permissionIntToString(permissionInt))
                .build()
        )
        responseObserver.onCompleted()
    }
}
