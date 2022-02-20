package info.skyblond.archivedag.arudaz.controller.grpc

import com.google.protobuf.ByteString
import info.skyblond.archivedag.ariteg.AritegService
import info.skyblond.archivedag.ariteg.model.BlobObject
import info.skyblond.archivedag.ariteg.utils.toMultihash
import info.skyblond.archivedag.arstue.FileRecordService
import info.skyblond.archivedag.arstue.GroupService
import info.skyblond.archivedag.arstue.UserManagementService
import info.skyblond.archivedag.arstue.entity.RecordAccessControlEntity
import info.skyblond.archivedag.arstue.repo.FileRecordRepository
import info.skyblond.archivedag.arstue.repo.RecordAccessControlRepository
import info.skyblond.archivedag.arudaz.model.ProtoReceipt
import info.skyblond.archivedag.arudaz.protos.common.Empty
import info.skyblond.archivedag.arudaz.protos.common.Page
import info.skyblond.archivedag.arudaz.protos.record.*
import info.skyblond.archivedag.arudaz.service.ApplicationConfigService
import info.skyblond.archivedag.arudaz.service.ProtoReceiptService
import info.skyblond.archivedag.commons.EntityNotFoundException
import info.skyblond.archivedag.commons.PermissionDeniedException
import info.skyblond.archivedag.commons.getUnixTimestamp
import info.skyblond.archivedag.safeExecutable
import io.grpc.internal.testing.StreamRecorder
import io.ipfs.multihash.Multihash
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.Pageable
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.ActiveProfiles
import java.util.*

@SpringBootTest
@ActiveProfiles("test")
internal class FileRecordControllerTest {
    @Autowired
    lateinit var fileRecordController: FileRecordController

    @Autowired
    lateinit var groupService: GroupService

    @Autowired
    lateinit var userManagementService: UserManagementService

    @Autowired
    lateinit var fileRecordRepository: FileRecordRepository

    @Autowired
    lateinit var recordAccessControlRepository: RecordAccessControlRepository

    @Autowired
    lateinit var applicationConfigService: ApplicationConfigService

    @Autowired
    lateinit var protoReceiptService: ProtoReceiptService

    @Autowired
    lateinit var fileRecordService: FileRecordService

    @Autowired
    lateinit var aritegService: AritegService

    @BeforeEach
    fun setUp() {
        safeExecutable { userManagementService.createUser("test_user_admin", "password") }
        safeExecutable { userManagementService.createUser("test_user", "password") }
        safeExecutable { userManagementService.deleteUser("test_user_404") }
        safeExecutable { groupService.deleteGroup("group_test_group") }
        safeExecutable { groupService.createGroup("group_test_group", "test_user") }
        safeExecutable { groupService.deleteGroup("group_test_group_admin") }
        safeExecutable { groupService.createGroup("group_test_group_admin", "test_user_admin") }
        safeExecutable { groupService.deleteGroup("group_test_group_404") }
        safeExecutable { disableGrpcWriteProto() }
    }

    @AfterEach
    fun tearDown() {
        safeExecutable { groupService.deleteGroup("group_test_group") }
        safeExecutable { groupService.deleteGroup("group_test_group_admin") }
        safeExecutable { groupService.deleteGroup("group_test_group_404") }
    }

    private fun enableGrpcWriteProto() {
        applicationConfigService.updateConfig(ApplicationConfigService.ALLOW_GRPC_WRITE_PROTO_KEY, "true")
    }

    private fun disableGrpcWriteProto() {
        applicationConfigService.updateConfig(ApplicationConfigService.ALLOW_GRPC_WRITE_PROTO_KEY, "false")
    }

    @WithMockUser(username = "test_user", roles = ["UPLOADER"])
    @Test
    fun testCreateFileRecord() {
        // test disabled config
        disableGrpcWriteProto()
        assertThrows<AccessDeniedException> {
            fileRecordController.createFileRecord(
                CreateFileRecordRequest.getDefaultInstance(), StreamRecorder.create()
            )
        }

        // with enabled config
        enableGrpcWriteProto()
        // test empty name
        assertThrows<IllegalArgumentException> {
            fileRecordController.createFileRecord(
                CreateFileRecordRequest.getDefaultInstance(), StreamRecorder.create()
            )
        }
        // test normal
        val request: CreateFileRecordRequest = CreateFileRecordRequest.newBuilder()
            .setRecordName("test create record")
            .build()
        val responseObserver: StreamRecorder<FileRecordUuidListResponse> = StreamRecorder.create()
        fileRecordController.createFileRecord(request, responseObserver)
        assertNull(responseObserver.error)
        assertEquals(1, responseObserver.values.size)
        val result: FileRecordUuidListResponse = responseObserver.values[0]
        assertEquals(1, result.recordUuidList.size)
        val createdRecordUUID = UUID.fromString(result.recordUuidList[0])
        assertTrue(fileRecordRepository.existsByRecordId(createdRecordUUID))
    }

    @WithMockUser(username = "test_user", roles = ["UPLOADER"])
    @Test
    fun testUpdateFileRecordRef() {
        // test disabled config
        disableGrpcWriteProto()
        assertThrows<AccessDeniedException> {
            fileRecordController.updateFileRecordRef(
                UpdateFileRecordRefRequest.getDefaultInstance(), StreamRecorder.create()
            )
        }

        // with enabled config
        enableGrpcWriteProto()
        // test invalid receipt (cannot decrypt)
        assertThrows<IllegalArgumentException> {
            fileRecordController.updateFileRecordRef(
                UpdateFileRecordRefRequest.newBuilder()
                    .setRecordUuid(UUID.randomUUID().toString())
                    .setReceipt("IA==.IA==")
                    .build(), StreamRecorder.create()
            )
        }
        // test invalid receipt (wrong user)
        assertThrows<PermissionDeniedException> {
            fileRecordController.updateFileRecordRef(
                UpdateFileRecordRefRequest.newBuilder()
                    .setRecordUuid(UUID.randomUUID().toString())
                    .setReceipt(
                        protoReceiptService.encryptReceipt(
                            ProtoReceipt(
                                UUID.randomUUID(),
                                "test_user_404",
                                Multihash(Multihash.Type.id, ByteArray(0))
                            )
                        )
                    )
                    .build(), StreamRecorder.create()
            )
        }
        // test invalid receipt (wrong record uuid)
        assertThrows<PermissionDeniedException> {
            fileRecordController.updateFileRecordRef(
                UpdateFileRecordRefRequest.newBuilder()
                    .setRecordUuid(UUID.randomUUID().toString())
                    .setReceipt(
                        protoReceiptService.encryptReceipt(
                            ProtoReceipt(
                                UUID.randomUUID(),
                                "test_user",
                                Multihash(Multihash.Type.id, ByteArray(0))
                            )
                        )
                    )
                    .build(), StreamRecorder.create()
            )
        }
        // test invalid receipt (missing multihash)
        UUID.randomUUID().let { uuid ->
            assertThrows<IllegalArgumentException> {
                fileRecordController.updateFileRecordRef(
                    UpdateFileRecordRefRequest.newBuilder()
                        .setRecordUuid(uuid.toString())
                        .setReceipt(
                            protoReceiptService.encryptReceipt(
                                ProtoReceipt(
                                    uuid,
                                    "test_user",
                                    Multihash(Multihash.Type.id, ByteArray(0))
                                )
                            )
                        )
                        .build(), StreamRecorder.create()
                )
            }
        }
        // test no update ref permission
        val multihash = aritegService.writeProto(
            "", BlobObject(ByteString.copyFrom("something", Charsets.UTF_8))
        ).let { it.completionFuture.get(); it.link.multihash.toMultihash() }
        fileRecordService.createRecord("test no update ref permission", "test_user_404")
            .let { uuid ->
                assertThrows<PermissionDeniedException> {
                    fileRecordController.updateFileRecordRef(
                        UpdateFileRecordRefRequest.newBuilder()
                            .setRecordUuid(uuid.toString())
                            .setReceipt(
                                protoReceiptService.encryptReceipt(
                                    ProtoReceipt(uuid, "test_user", multihash)
                                )
                            )
                            .build(), StreamRecorder.create()
                    )
                }
            }

        // test null -> initial
        val recordUUID = fileRecordService.createRecord("test normal update", "test_user")
        assertNull(fileRecordRepository.findByRecordId(recordUUID)!!.multihash)
        var responseObserver: StreamRecorder<Empty> = StreamRecorder.create()
        fileRecordController.updateFileRecordRef(
            UpdateFileRecordRefRequest.newBuilder()
                .setRecordUuid(recordUUID.toString())
                .setReceipt(
                    protoReceiptService.encryptReceipt(ProtoReceipt(recordUUID, "test_user", multihash))
                )
                .build(), responseObserver
        )
        checkEmptyResponse(responseObserver)
        var link = aritegService.parseMultihash(
            Multihash.fromBase58(fileRecordRepository.findByRecordId(recordUUID)!!.multihash!!)
        )!!
        var commit = aritegService.readCommit(link)
        assertTrue(commit.parentLink.multihash.isEmpty)
        assertEquals(multihash, commit.committedObjectLink.multihash.toMultihash())
        assertEquals("test_user", aritegService.readBlob(commit.authorLink).data.toStringUtf8())

        // test append to old
        responseObserver = StreamRecorder.create()
        fileRecordController.updateFileRecordRef(
            UpdateFileRecordRefRequest.newBuilder()
                .setRecordUuid(recordUUID.toString())
                .setReceipt(
                    protoReceiptService.encryptReceipt(ProtoReceipt(recordUUID, "test_user", multihash))
                )
                .build(), responseObserver
        )
        checkEmptyResponse(responseObserver)
        val oldLink = link
        link = aritegService.parseMultihash(
            Multihash.fromBase58(fileRecordRepository.findByRecordId(recordUUID)!!.multihash!!)
        )!!
        commit = aritegService.readCommit(link)
        assertTrue(commit.parentLink.multihash == oldLink.multihash)
        assertEquals(multihash, commit.committedObjectLink.multihash.toMultihash())
        assertEquals("test_user", aritegService.readBlob(commit.authorLink).data.toStringUtf8())
    }

    @WithMockUser(username = "test_user", roles = ["UPLOADER"])
    @Test
    fun testUpdateFileRecordName() {
        // test no update name permission
        fileRecordService.createRecord("name", "test_user_404")
            .let { uuid ->
                val request = UpdateFileRecordNameRequest.newBuilder()
                    .setRecordUuid(uuid.toString())
                    .build()
                assertThrows<PermissionDeniedException> {
                    fileRecordController.updateFileRecordName(request, StreamRecorder.create())
                }
            }
        // test empty name
        val recordUUID = fileRecordService.createRecord("name", "test_user")
        assertThrows<IllegalArgumentException> {
            fileRecordController.updateFileRecordName(
                UpdateFileRecordNameRequest.newBuilder()
                    .setRecordUuid(recordUUID.toString())
                    .setNewRecordName("")
                    .build(), StreamRecorder.create()
            )
        }
        // test normal rename
        val resp = StreamRecorder.create<Empty>()
        fileRecordController.updateFileRecordName(
            UpdateFileRecordNameRequest.newBuilder()
                .setRecordUuid(recordUUID.toString())
                .setNewRecordName("new name")
                .build(), resp
        )
        checkEmptyResponse(resp)
        assertEquals("new name", fileRecordRepository.findByRecordId(recordUUID)!!.recordName)
    }

    @WithMockUser(username = "test_user", roles = ["UPLOADER"])
    @Test
    fun testTransferFileRecordUser() {
        // transfer 404 record
        TransferFileRecordRequest.newBuilder()
            .setRecordUuid(UUID.randomUUID().toString())
            .build()
            .let { request ->
                assertThrows<EntityNotFoundException> {
                    fileRecordController.transferFileRecord(request, StreamRecorder.create())
                }
            }
        // transfer other's record
        fileRecordService.createRecord("test transfer other's record", "test_user_404")
            .let { uuid ->
                val request = TransferFileRecordRequest.newBuilder()
                    .setRecordUuid(uuid.toString())
                    .build()
                assertThrows<PermissionDeniedException> {
                    fileRecordController.transferFileRecord(request, StreamRecorder.create())
                }
            }
        // transfer to 404 user
        fileRecordService.createRecord("test transfer record to 404 user", "test_user")
            .let { uuid ->
                val request = TransferFileRecordRequest.newBuilder()
                    .setRecordUuid(uuid.toString())
                    .setNewOwner("test_user_404")
                    .build()
                assertThrows<IllegalArgumentException> {
                    fileRecordController.transferFileRecord(request, StreamRecorder.create())
                }
            }
        // normal transfer
        fileRecordService.createRecord("test normal transfer record", "test_user")
            .let { uuid ->
                val request = TransferFileRecordRequest.newBuilder()
                    .setRecordUuid(uuid.toString())
                    .setNewOwner("test_user_admin")
                    .build()
                val resp = StreamRecorder.create<Empty>()
                fileRecordController.transferFileRecord(request, resp)
                checkEmptyResponse(resp)
            }
    }

    @WithMockUser(username = "test_user_admin", roles = ["ADMIN"])
    @Test
    fun testTransferFileRecordAdmin() {
        // transfer 404 record
        TransferFileRecordRequest.newBuilder()
            // here the check in controller will be shorted
            // but the check in service (setNewOwner) will kick in
            .setRecordUuid(UUID.randomUUID().toString())
            .setNewOwner("test_user_admin")
            .build()
            .let { request ->
                assertThrows<EntityNotFoundException> {
                    fileRecordController.transferFileRecord(request, StreamRecorder.create())
                }
            }
        // transfer other's record
        fileRecordService.createRecord("test transfer other's record", "test_user_404")
            .let { uuid ->
                val request = TransferFileRecordRequest.newBuilder()
                    .setRecordUuid(uuid.toString())
                    .setNewOwner("test_user_admin")
                    .build()
                val resp = StreamRecorder.create<Empty>()
                fileRecordController.transferFileRecord(request, resp)
                checkEmptyResponse(resp)
            }
        // transfer to 404 user
        fileRecordService.createRecord("test transfer record to 404 user", "test_user")
            .let { uuid ->
                val request = TransferFileRecordRequest.newBuilder()
                    .setRecordUuid(uuid.toString())
                    .setNewOwner("test_user_404")
                    .build()
                assertThrows<IllegalArgumentException> {
                    fileRecordController.transferFileRecord(request, StreamRecorder.create())
                }
            }
    }

    @WithMockUser(username = "test_user", roles = ["UPLOADER"])
    @Test
    fun testDeleteFileRecordUser() {
        // test disabled config
        UUID.randomUUID().let { uuid ->
            val request = DeleteFileRecordRequest.newBuilder()
                .setRecordUuid(uuid.toString())
                .build()
            assertThrows<AccessDeniedException> {
                fileRecordController.deleteFileRecord(request, StreamRecorder.create())
            }
        }
        // enable config
        enableGrpcWriteProto()
        // test delete 404 record
        UUID.randomUUID().let { uuid ->
            val request = DeleteFileRecordRequest.newBuilder()
                .setRecordUuid(uuid.toString())
                .build()
            assertThrows<EntityNotFoundException> {
                fileRecordController.deleteFileRecord(request, StreamRecorder.create())
            }
        }
        // test delete others
        fileRecordService.createRecord("test delete others", "test_user_404")
            .let { uuid ->
                val request = DeleteFileRecordRequest.newBuilder()
                    .setRecordUuid(uuid.toString())
                    .build()
                assertThrows<PermissionDeniedException> {
                    fileRecordController.deleteFileRecord(request, StreamRecorder.create())
                }
            }
        // normal delete
        fileRecordService.createRecord("test delete record normal", "test_user")
            .let { uuid ->
                val request = DeleteFileRecordRequest.newBuilder()
                    .setRecordUuid(uuid.toString())
                    .build()
                val resp = StreamRecorder.create<Empty>()
                fileRecordController.deleteFileRecord(request, resp)
                checkEmptyResponse(resp)
            }
    }

    @WithMockUser(username = "test_user_admin", roles = ["ADMIN"])
    @Test
    fun testDeleteFileRecordAdmin() {
        // test disabled config
        UUID.randomUUID().let { uuid ->
            val request = DeleteFileRecordRequest.newBuilder()
                .setRecordUuid(uuid.toString())
                .build()
            assertThrows<AccessDeniedException> {
                fileRecordController.deleteFileRecord(request, StreamRecorder.create())
            }
        }
        // enable config
        enableGrpcWriteProto()
        // test delete 404 record
        UUID.randomUUID().let { uuid ->
            val request = DeleteFileRecordRequest.newBuilder()
                .setRecordUuid(uuid.toString())
                .build()
            assertThrows<EntityNotFoundException> {
                fileRecordController.deleteFileRecord(request, StreamRecorder.create())
            }
        }
        // test delete others
        fileRecordService.createRecord("test delete others", "test_user_404")
            .let { uuid ->
                val request = DeleteFileRecordRequest.newBuilder()
                    .setRecordUuid(uuid.toString())
                    .build()
                val resp = StreamRecorder.create<Empty>()
                fileRecordController.deleteFileRecord(request, resp)
                checkEmptyResponse(resp)
            }
    }

    @WithMockUser(username = "test_user", roles = ["VIEWER"])
    @Test
    fun testQueryFileRecord() {
        // test no r permission, assuming we don;t have r permission on missing record
        UUID.randomUUID().let { uuid ->
            val request = QueryFileRecordRequest.newBuilder()
                .setRecordUuid(uuid.toString())
                .build()
            assertThrows<PermissionDeniedException> {
                fileRecordController.queryFileRecord(request, StreamRecorder.create())
            }
        }
        // test normal query, we have r permission since we're the owner
        fileRecordService.createRecord("test normal query record", "test_user").let { uuid ->
            val request = QueryFileRecordRequest.newBuilder()
                .setRecordUuid(uuid.toString())
                .build()
            val resp = StreamRecorder.create<FileRecordDetailResponse>()
            fileRecordController.queryFileRecord(request, resp)
            assertNull(resp.error)
            assertEquals(1, resp.values.size)
            val result = resp.values[0]
            val entity = fileRecordRepository.findByRecordId(uuid)!!
            assertEquals(uuid.toString(), result.recordUuid)
            assertEquals(entity.recordName, result.recordName)
            if (entity.multihash != null) {
                val receipt = protoReceiptService.decryptReceipt(result.receipt)!!
                assertEquals(entity.multihash, receipt.primaryHash)
                assertEquals(uuid, receipt.recordId)
                assertEquals("test_user", receipt.username)
            }
            assertEquals(getUnixTimestamp(entity.createdTime.time), result.createdTimestamp)
            assertEquals(entity.owner, result.owner)
        }
    }

    @WithMockUser(username = "test_user", roles = ["VIEWER"])
    @Test
    fun testListOwnedFileRecords() {
        fileRecordService.createRecord("test list owned file", "test_user")
        val expected = fileRecordRepository.findAllByOwner("test_user", Pageable.unpaged())
            .map { it.recordId!!.toString() }
        val request = Page.newBuilder().setPage(0).setSize(expected.size).build()
        val response = StreamRecorder.create<FileRecordUuidListResponse>()
        fileRecordController.listOwnedFileRecords(request, response)
        assertNull(response.error)
        assertEquals(1, response.values.size)
        val result = response.values[0]
        assertArrayEquals(
            expected.toList().toTypedArray(),
            result.recordUuidList.map { it.toString() }.toTypedArray()
        )
    }

    @WithMockUser(username = "test_user", roles = ["VIEWER"])
    @Test
    fun testListUserSharedFileRecords() {
        val uuid = fileRecordService.createRecord("test list user shared records", "test_user_404")
        fileRecordService.setAccessRule(
            uuid,
            RecordAccessControlEntity.Type.USER, "test_user",
            FileRecordService.permissionStringToInt("r")
        )
        val request = Page.newBuilder().setPage(0).setSize(20).build()
        val response = StreamRecorder.create<FileRecordUuidListResponse>()
        fileRecordController.listUserSharedFileRecords(request, response)
        assertNull(response.error)
        assertEquals(1, response.values.size)
        val result = response.values[0]
        assertEquals(1, result.recordUuidList.size)
        assertEquals(uuid.toString(), result.recordUuidList[0].toString())
    }

    @WithMockUser(username = "test_user", roles = ["VIEWER"])
    @Test
    fun testListGroupSharedFileRecords() {
        // test not related to the group
        val uuid = fileRecordService.createRecord("test list group shared records", "test_user_404")
        fileRecordService.setAccessRule(
            uuid,
            RecordAccessControlEntity.Type.GROUP, "group_test_group_admin",
            FileRecordService.permissionStringToInt("r")
        ) // note the user "test_user" is not related to the "group_test_group_admin" by default
        var request = ListGroupSharedFileRecordsRequest.newBuilder()
            .setGroupName("group_test_group_admin")
            .setPagination(Page.newBuilder().setPage(0).setSize(20).build())
            .build()
        assertThrows<PermissionDeniedException> {
            fileRecordController.listGroupSharedFileRecords(request, StreamRecorder.create())
        }
        // test is group member
        groupService.addUserToGroup("group_test_group_admin", "test_user")
        var response = StreamRecorder.create<FileRecordUuidListResponse>()
        fileRecordController.listGroupSharedFileRecords(request, response)
        assertNull(response.error)
        assertEquals(1, response.values.size)
        var result = response.values[0]
        assertEquals(1, result.recordUuidList.size)
        assertEquals(uuid.toString(), result.recordUuidList[0])
        // test is group owner
        fileRecordService.setAccessRule(
            uuid,
            RecordAccessControlEntity.Type.GROUP, "group_test_group",
            FileRecordService.permissionStringToInt("r")
        )
        request = ListGroupSharedFileRecordsRequest.newBuilder()
            .setGroupName("group_test_group")
            .setPagination(Page.newBuilder().setPage(0).setSize(20).build())
            .build()
        response = StreamRecorder.create()
        fileRecordController.listGroupSharedFileRecords(request, response)
        assertNull(response.error)
        assertEquals(1, response.values.size)
        result = response.values[0]
        assertEquals(1, result.recordUuidList.size)
        assertEquals(uuid.toString(), result.recordUuidList[0])
    }

    @WithMockUser(username = "test_user", roles = ["VIEWER"])
    @Test
    fun testListPublicSharedFileRecords() {
        val uuid = fileRecordService.createRecord("test list public shared records", "test_user_404")
        fileRecordService.setAccessRule(
            uuid,
            RecordAccessControlEntity.Type.OTHER, "",
            FileRecordService.permissionStringToInt("r")
        )

        val expected = recordAccessControlRepository.findAllByTypeAndTargetAndPermissionContains(
            RecordAccessControlEntity.Type.OTHER, "", "r", Pageable.unpaged()
        ).map { it.recordId.toString() }.toList().toTypedArray()

        val request = Page.newBuilder().setPage(0).setSize(expected.size).build()
        val response = StreamRecorder.create<FileRecordUuidListResponse>()
        fileRecordController.listPublicSharedFileRecords(request, response)
        assertNull(response.error)
        assertEquals(1, response.values.size)
        val result = response.values[0]
        println(result.recordUuidList)
        assertEquals(expected.size, result.recordUuidList.size)
        assertArrayEquals(expected, result.recordUuidList.toTypedArray())
    }

    @WithMockUser(username = "test_user", roles = ["UPLOADER"])
    @Test
    fun testSetSharedRuleForRecord() {
        // test set on 404 record
        UUID.randomUUID().let { uuid ->
            val request = SharedRule.newBuilder()
                .setRecordUuid(uuid.toString())
                .build()
            assertThrows<EntityNotFoundException> {
                fileRecordController.setSharedRuleForRecord(request, StreamRecorder.create())
            }
        }
        // test set but not owner
        fileRecordService.createRecord("test set not owner", "test_user_404").let { uuid ->
            val request = SharedRule.newBuilder()
                .setRecordUuid(uuid.toString())
                .build()
            assertThrows<PermissionDeniedException> {
                fileRecordController.setSharedRuleForRecord(request, StreamRecorder.create())
            }
        }
        // test target not found
        fileRecordService.createRecord("test set 404 target", "test_user").let { uuid ->
            var request = SharedRule.newBuilder()
                .setRecordUuid(uuid.toString())
                .setRuleType(SharedRuleType.USER)
                .setRuleTarget("test_user_404")
                .build()
            assertThrows<IllegalArgumentException> {
                fileRecordController.setSharedRuleForRecord(request, StreamRecorder.create())
            }
            request = SharedRule.newBuilder()
                .setRecordUuid(uuid.toString())
                .setRuleType(SharedRuleType.GROUP)
                .setRuleTarget("group_test_group_404")
                .build()
            assertThrows<IllegalArgumentException> {
                fileRecordController.setSharedRuleForRecord(request, StreamRecorder.create())
            }
        }
        // test normal set
        fileRecordService.createRecord("test normal set", "test_user").let { uuid ->
            val request = SharedRule.newBuilder()
                .setRecordUuid(uuid.toString())
                .setRuleType(SharedRuleType.USER)
                .setRuleTarget("test_user_admin")
                .setPermission("rh")
                .build()
            val response = StreamRecorder.create<Empty>()
            fileRecordController.setSharedRuleForRecord(request, response)
            checkEmptyResponse(response)
        }
    }

    @WithMockUser(username = "test_user", roles = ["UPLOADER"])
    @Test
    fun testDeleteSharedRuleForRecord() {
        // test delete on 404 record
        UUID.randomUUID().let { uuid ->
            val request = DeleteSharedRuleForRecordRequest.newBuilder()
                .setRecordUuid(uuid.toString())
                .build()
            assertThrows<EntityNotFoundException> {
                fileRecordController.deleteSharedRuleForRecord(request, StreamRecorder.create())
            }
        }
        // test delete but not owner
        fileRecordService.createRecord("test delete rules", "test_user_404").let { uuid ->
            val request = DeleteSharedRuleForRecordRequest.newBuilder()
                .setRecordUuid(uuid.toString())
                .build()
            assertThrows<PermissionDeniedException> {
                fileRecordController.deleteSharedRuleForRecord(request, StreamRecorder.create())
            }
        }
        // test delete 404 rule
        fileRecordService.createRecord("test delete 404 rules", "test_user").let { uuid ->
            val request = DeleteSharedRuleForRecordRequest.newBuilder()
                .setRecordUuid(uuid.toString())
                .setRuleType(SharedRuleType.OTHER)
                .build()
            assertThrows<EntityNotFoundException> {
                fileRecordController.deleteSharedRuleForRecord(request, StreamRecorder.create())
            }
        }
        // test normal delete
        fileRecordService.createRecord("test delete 404 rules", "test_user").let { uuid ->
            recordAccessControlRepository.save(
                RecordAccessControlEntity(
                    uuid,
                    RecordAccessControlEntity.Type.OTHER,
                    "",
                    "r"
                )
            )
            val request = DeleteSharedRuleForRecordRequest.newBuilder()
                .setRecordUuid(uuid.toString())
                .setRuleType(SharedRuleType.OTHER)
                .build()
            val response = StreamRecorder.create<Empty>()
            fileRecordController.deleteSharedRuleForRecord(request, response)
            checkEmptyResponse(response)
            assertFalse(
                recordAccessControlRepository.existsByRecordIdAndTypeAndTarget(
                    uuid, RecordAccessControlEntity.Type.OTHER, ""
                )
            )
        }
    }

    @WithMockUser(username = "test_user", roles = ["VIEWER"])
    @Test
    fun testListSharedRulesForRecord() {
        // test list 404 record
        UUID.randomUUID().let { uuid ->
            val request = ListSharedRulesForRecordRequest.newBuilder()
                .setRecordUuid(uuid.toString())
                .setPagination(Page.newBuilder().setPage(0).setSize(20).build())
                .build()
            assertThrows<EntityNotFoundException> {
                fileRecordController.listSharedRulesForRecord(request, StreamRecorder.create())
            }
        }
        // test no permission
        fileRecordService.createRecord("test list others rules", "test_user_404").let { uuid ->
            val request = ListSharedRulesForRecordRequest.newBuilder()
                .setRecordUuid(uuid.toString())
                .setPagination(Page.newBuilder().setPage(0).setSize(20).build())
                .build()
            assertThrows<PermissionDeniedException> {
                fileRecordController.listSharedRulesForRecord(request, StreamRecorder.create())
            }
        }
        // normal list
        fileRecordService.createRecord("test list rules", "test_user").let { uuid ->
            recordAccessControlRepository.save(
                RecordAccessControlEntity(
                    uuid,
                    RecordAccessControlEntity.Type.OTHER,
                    "",
                    "rhun"
                )
            )
            val request = ListSharedRulesForRecordRequest.newBuilder()
                .setRecordUuid(uuid.toString())
                .setPagination(Page.newBuilder().setPage(0).setSize(20).build())
                .build()
            val response = StreamRecorder.create<SharedRuleListResponse>()
            fileRecordController.listSharedRulesForRecord(request, response)
            assertNull(response.error)
            assertEquals(1, response.values.size)
            val result = response.values[0]
            assertEquals(1, result.sharedRuleList.size)
            assertEquals(uuid.toString(), result.sharedRuleList[0].recordUuid)
            assertEquals(SharedRuleType.OTHER, result.sharedRuleList[0].ruleType)
            assertEquals("", result.sharedRuleList[0].ruleTarget)
            assertEquals("rhun", result.sharedRuleList[0].permission)
        }
    }

    @WithMockUser(username = "test_user", roles = ["VIEWER"])
    @Test
    fun testQueryMyPermissionOnRecord() {
        fileRecordService.createRecord("test query permisison", "test_user").let { uuid ->
            val request = QueryMyPermissionOnRecordRequest.newBuilder()
                .setRecordUuid(uuid.toString())
                .build()
            val response = StreamRecorder.create<SharedRulePermissionResponse>()
            fileRecordController.queryMyPermissionOnRecord(request, response)
            assertNull(response.error)
            assertEquals(1, response.values.size)
            val result = response.values[0].permission
            assertEquals(
                FileRecordService.permissionIntToString(FileRecordService.FULL_PERMISSION),
                result
            )
        }
    }
}
