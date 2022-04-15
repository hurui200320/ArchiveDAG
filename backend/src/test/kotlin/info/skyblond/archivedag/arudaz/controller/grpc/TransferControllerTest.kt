package info.skyblond.archivedag.arudaz.controller.grpc

import com.google.protobuf.ByteString
import info.skyblond.archivedag.ariteg.AritegService
import info.skyblond.archivedag.ariteg.config.AritegConfiguration
import info.skyblond.archivedag.ariteg.model.BlobObject
import info.skyblond.archivedag.ariteg.model.CommitObject
import info.skyblond.archivedag.ariteg.multihash.MultihashProvider
import info.skyblond.archivedag.ariteg.multihash.MultihashProviders
import info.skyblond.archivedag.ariteg.protos.AritegLink
import info.skyblond.archivedag.ariteg.protos.AritegListObject
import info.skyblond.archivedag.ariteg.protos.AritegObjectType
import info.skyblond.archivedag.ariteg.protos.AritegTreeObject
import info.skyblond.archivedag.ariteg.utils.toMultihash
import info.skyblond.archivedag.arstue.FileRecordService
import info.skyblond.archivedag.arstue.GroupService
import info.skyblond.archivedag.arstue.UserManagementService
import info.skyblond.archivedag.arudaz.model.TransferReceipt
import info.skyblond.archivedag.arudaz.protos.common.Empty
import info.skyblond.archivedag.arudaz.protos.transfer.*
import info.skyblond.archivedag.arudaz.service.ApplicationConfigService
import info.skyblond.archivedag.arudaz.service.TransferReceiptService
import info.skyblond.archivedag.commons.EntityNotFoundException
import info.skyblond.archivedag.commons.getUnixTimestamp
import info.skyblond.archivedag.safeExecutable
import io.grpc.internal.testing.StreamRecorder
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.ActiveProfiles
import kotlin.random.Random

@SpringBootTest
@ActiveProfiles("test")
internal class TransferControllerTest {

    @Autowired
    lateinit var transferController: TransferController

    @Autowired
    lateinit var groupService: GroupService

    @Autowired
    lateinit var userManagementService: UserManagementService

    @Autowired
    lateinit var applicationConfigService: ApplicationConfigService

    @Autowired
    lateinit var aritegConfiguration: AritegConfiguration

    @Autowired
    lateinit var fileRecordService: FileRecordService

    @Autowired
    lateinit var transferReceiptService: TransferReceiptService

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
        safeExecutable {
            applicationConfigService.updateConfig(
                ApplicationConfigService.ALLOW_GRPC_WRITE_PROTO_KEY,
                "true"
            )
        }
    }

    @WithMockUser(username = "test_user", roles = ["VIEWER"])
    @Test
    fun queryServerProtoConfig() {
        val resp = StreamRecorder.create<QueryServerProtoConfigResponse>()
        transferController.queryServerProtoConfig(Empty.getDefaultInstance(), resp)
        assertNull(resp.error)
        assertEquals(1, resp.values.size)
        val result = resp.values[0]
        assertEquals(aritegConfiguration.getPrimaryHashType().toString(), result.primaryHashType)
        assertEquals(aritegConfiguration.getSecondaryHashType().toString(), result.secondaryHashType)
    }

    private fun getPrimaryMultihashProvider(): MultihashProvider {
        return MultihashProviders.fromMultihashType(aritegConfiguration.getPrimaryHashType())
    }

    private fun getSecondaryMultihashProvider(): MultihashProvider {
        return MultihashProviders.fromMultihashType(aritegConfiguration.getSecondaryHashType())
    }

    @WithMockUser(username = "test_user", roles = ["UPLOADER", "VIEWER"])
    @Test
    fun uploadAndReadBlob() {
        val uuid = fileRecordService.createRecord("test blob", "test_user")
        val blobContent = ByteString.copyFrom("Something", Charsets.UTF_8)
        val blobObj = BlobObject(blobContent).toProto()
        val blobMultihash = getPrimaryMultihashProvider().digest(blobObj.toByteArray())

        val uploadRequest = UploadBlobRequest.newBuilder()
            .setRecordUuid(uuid.toString())
            .setBlobObj(blobObj)
            .setPrimaryHash(ByteString.copyFrom(blobMultihash.toBytes()))
            .build()
        val uploadResponse = StreamRecorder.create<TransferReceiptResponse>()
        transferController.uploadBlob(uploadRequest, uploadResponse)
        assertNull(uploadResponse.error)
        assertEquals(1, uploadResponse.values.size)
        val uploadResult = uploadResponse.values[0]
        val uploadMultihash = uploadResult.primaryHash.toMultihash()
        assertEquals(blobMultihash, uploadMultihash)
        val uploadReceipt = uploadResult.transferReceipt
        transferReceiptService.decryptReceipt(uploadReceipt).let {
            assertNotNull(it)
            assertEquals(uploadMultihash, it!!.primaryHash)
            assertEquals("test_user", it.username)
            assertEquals(uuid, it.recordId)
        }

        val downloadRequest = ReadObjectRequest.newBuilder()
            .setTransferReceipt(uploadReceipt)
            .build()
        val downloadResponse = StreamRecorder.create<ReadBlobResponse>()
        transferController.readBlob(downloadRequest, downloadResponse)
        assertNull(downloadResponse.error)
        assertEquals(1, downloadResponse.values.size)
        val downloadResult = downloadResponse.values[0]
        assertEquals(uploadMultihash, downloadResult.primaryHash.toMultihash())
        assertEquals(blobContent, downloadResult.blobObj.data)
    }

    @WithMockUser(username = "test_user", roles = ["UPLOADER", "VIEWER"])
    @Test
    fun uploadAndReadList() {
        val uuid = fileRecordService.createRecord("test list", "test_user")

        val blobList = buildList {
            val list = (1..10).map {
                aritegService.writeProto(
                    "",
                    BlobObject(ByteString.copyFrom("Something...#$it", Charsets.UTF_8))
                )
            }
            addAll(list.map {
                val link = it.link.toBuilder()
                val receipt = transferReceiptService.encryptReceipt(
                    TransferReceipt(
                        uuid,
                        "test_user",
                        it.link.multihash.toMultihash(),
                        AritegObjectType.BLOB
                    )
                )
                link.setMultihash(ByteString.copyFrom(receipt, Charsets.UTF_8)).build()
            })
            list.map { it.completionFuture }.forEach { it.get() }
        }
        val listObj = AritegListObject.newBuilder().addAllLinks(blobList).build()
        val originalMultihash = getPrimaryMultihashProvider().digest(listObj.toByteArray())

        val uploadRequest = UploadListRequest.newBuilder()
            .setRecordUuid(uuid.toString())
            .setListObj(listObj)
            .setPrimaryHash(ByteString.copyFrom(originalMultihash.toBytes()))
            .build()
        val uploadResponse = StreamRecorder.create<TransferReceiptResponse>()
        transferController.uploadList(uploadRequest, uploadResponse)
        assertNull(uploadResponse.error)
        assertEquals(1, uploadResponse.values.size)
        val uploadResult = uploadResponse.values[0]
        val uploadMultihash = uploadResult.primaryHash.toMultihash()
        assertNotEquals(originalMultihash, uploadMultihash)
        val uploadReceipt = uploadResult.transferReceipt
        transferReceiptService.decryptReceipt(uploadReceipt).let {
            assertNotNull(it)
            assertEquals(uploadMultihash, it!!.primaryHash)
            assertEquals("test_user", it.username)
            assertEquals(uuid, it.recordId)
        }

        val downloadRequest = ReadObjectRequest.newBuilder()
            .setTransferReceipt(uploadReceipt)
            .build()
        val downloadResponse = StreamRecorder.create<ReadListResponse>()
        transferController.readList(downloadRequest, downloadResponse)
        assertNull(downloadResponse.error)
        assertEquals(1, downloadResponse.values.size)
        val downloadResult = downloadResponse.values[0]
        assertEquals(uploadMultihash, downloadResult.primaryHash.toMultihash())
        assertArrayEquals(
            blobList.map { transferReceiptService.decryptReceipt(it.multihash.toStringUtf8()) to it.type }
                .toTypedArray(),
            downloadResult.listObj.linksList.map { transferReceiptService.decryptReceipt(it.multihash.toStringUtf8()) to it.type }
                .toTypedArray()
        )
    }

    @WithMockUser(username = "test_user", roles = ["UPLOADER", "VIEWER"])
    @Test
    fun uploadAndReadTree() {
        val uuid = fileRecordService.createRecord("test tree", "test_user")

        val blobList = buildList {
            val list = (1..10).map {
                aritegService.writeProto(
                    "blob #$it",
                    BlobObject(ByteString.copyFrom("Something...#$it", Charsets.UTF_8))
                )
            }
            addAll(list.map {
                val link = it.link.toBuilder()
                val receipt = transferReceiptService.encryptReceipt(
                    TransferReceipt(
                        uuid, "test_user",
                        it.link.multihash.toMultihash(),
                        AritegObjectType.BLOB
                    )
                )
                link.setMultihash(ByteString.copyFrom(receipt, Charsets.UTF_8)).build()
            })
            list.map { it.completionFuture }.forEach { it.get() }
        }
        val treeObj = AritegTreeObject.newBuilder().addAllLinks(blobList).build()
        val originalMultihash = getPrimaryMultihashProvider().digest(treeObj.toByteArray())

        val uploadRequest = UploadTreeRequest.newBuilder()
            .setRecordUuid(uuid.toString())
            .setTreeObj(treeObj)
            .setPrimaryHash(ByteString.copyFrom(originalMultihash.toBytes()))
            .build()
        val uploadResponse = StreamRecorder.create<TransferReceiptResponse>()
        transferController.uploadTree(uploadRequest, uploadResponse)
        assertNull(uploadResponse.error)
        assertEquals(1, uploadResponse.values.size)
        val uploadResult = uploadResponse.values[0]
        val uploadMultihash = uploadResult.primaryHash.toMultihash()
        assertNotEquals(originalMultihash, uploadMultihash)
        val uploadReceipt = uploadResult.transferReceipt
        transferReceiptService.decryptReceipt(uploadReceipt).let {
            assertNotNull(it)
            assertEquals(uploadMultihash, it!!.primaryHash)
            assertEquals("test_user", it.username)
            assertEquals(uuid, it.recordId)
        }

        val downloadRequest = ReadObjectRequest.newBuilder()
            .setTransferReceipt(uploadReceipt)
            .build()
        val downloadResponse = StreamRecorder.create<ReadTreeResponse>()
        transferController.readTree(downloadRequest, downloadResponse)
        assertNull(downloadResponse.error)
        assertEquals(1, downloadResponse.values.size)
        val downloadResult = downloadResponse.values[0]
        assertEquals(uploadMultihash, downloadResult.primaryHash.toMultihash())
        assertArrayEquals(
            blobList.map {
                transferReceiptService.decryptReceipt(it.multihash.toStringUtf8()) to it.type to it.name
            }.toTypedArray(),
            downloadResult.treeObj.linksList.map {
                transferReceiptService.decryptReceipt(it.multihash.toStringUtf8()) to it.type to it.name
            }.toTypedArray()
        )
    }

    @WithMockUser(username = "test_user", roles = ["VIEWER"])
    @Test
    fun proveOwnership() {
        val uuid = fileRecordService.createRecord("test ownership", "test_user")
        val blobContent = ByteString.copyFrom(Random.nextBytes(256))
        val blobObj = BlobObject(blobContent).toProto()
        val blobPriMultihash = getPrimaryMultihashProvider().digest(blobObj.toByteArray())
        val blobSecMultihash = getSecondaryMultihashProvider().digest(blobObj.toByteArray())

        val proveRequest = ProveOwnershipRequest.newBuilder()
            .setRecordUuid(uuid.toString())
            .setPrimaryHash(ByteString.copyFrom(blobPriMultihash.toBytes()))
            .setSecondaryHash(ByteString.copyFrom(blobSecMultihash.toBytes()))
            .build()
        // proto not found
        assertThrows<EntityNotFoundException> {
            transferController.proveOwnership(proveRequest, StreamRecorder.create())
        }
        val writeReceipt = aritegService.writeProto("", BlobObject.fromProto(blobObj))
        // proto found
        val proveResponse = StreamRecorder.create<TransferReceiptResponse>()
        writeReceipt.completionFuture.get()
        transferController.proveOwnership(proveRequest, proveResponse)
        assertNull(proveResponse.error)
        assertEquals(1, proveResponse.values.size)
        val proveResult = proveResponse.values[0]
        assertEquals(blobPriMultihash, proveResult.primaryHash.toMultihash())
        transferReceiptService.decryptReceipt(proveResult.transferReceipt).let {
            assertNotNull(it)
            assertEquals(uuid, it!!.recordId)
            assertEquals("test_user", it.username)
            assertEquals(blobPriMultihash, it.primaryHash)
        }
    }

    @WithMockUser(username = "test_user", roles = ["VIEWER"])
    @Test
    fun readCommit() {
        val uuid = fileRecordService.createRecord("test commit", "test_user")
        val blobObj = BlobObject(ByteString.copyFrom(Random.nextBytes(16)))
        val blobWriteReceipt = aritegService.writeProto("", blobObj)
        val authorObj = BlobObject(ByteString.copyFrom(Random.nextBytes(16)))
        val authorWriteReceipt = aritegService.writeProto("", authorObj)
        val commitObj = CommitObject(
            unixTimestamp = getUnixTimestamp(),
            message = "initial commit",
            parentLink = AritegLink.newBuilder().setType(AritegObjectType.COMMIT).build(),
            committedObjectLink = blobWriteReceipt.link,
            authorLink = authorWriteReceipt.link,
        )
        blobWriteReceipt.completionFuture.get()
        authorWriteReceipt.completionFuture.get()
        val commitWriteReceipt = aritegService.writeProto("", commitObj)
        val transferReceipt = transferReceiptService.encryptReceipt(
            TransferReceipt(
                uuid, "test_user",
                commitWriteReceipt.link.multihash.toMultihash(),
                AritegObjectType.COMMIT
            )
        )

        // read the commit
        val request = ReadObjectRequest.newBuilder()
            .setTransferReceipt(transferReceipt)
            .build()
        val response = StreamRecorder.create<ReadCommitResponse>()
        commitWriteReceipt.completionFuture.get()
        transferController.readCommit(request, response)
        assertNull(response.error)
        assertEquals(1, response.values.size)
        val result = response.values[0]
        assertEquals(commitWriteReceipt.link.multihash.toMultihash(), result.primaryHash.toMultihash())
        assertEquals(commitObj.unixTimestamp, result.commitObj.unixTimestamp)
        assertEquals(commitObj.message, result.commitObj.message)
        assertTrue(result.commitObj.parent.multihash.isEmpty)
        transferReceiptService.decryptReceipt(result.commitObj.committedObject.multihash.toStringUtf8()).let {
            assertNotNull(it)
            assertEquals(uuid, it!!.recordId)
            assertEquals("test_user", it.username)
            assertEquals(blobWriteReceipt.link.multihash.toMultihash(), it.primaryHash)
        }
        transferReceiptService.decryptReceipt(result.commitObj.author.multihash.toStringUtf8()).let {
            assertNotNull(it)
            assertEquals(uuid, it!!.recordId)
            assertEquals("test_user", it.username)
            assertEquals(authorWriteReceipt.link.multihash.toMultihash(), it.primaryHash)
        }
    }

    @WithMockUser(username = "test_user", roles = ["VIEWER"])
    @Test
    fun validateObject() {
        val uuid = fileRecordService.createRecord("test valide", "test_user")
        val blobContent = ByteString.copyFrom(Random.nextBytes(256))
        val blobObj = BlobObject(blobContent)
        aritegService.writeProto("", blobObj).completionFuture.get()
        val blobPriMultihash = getPrimaryMultihashProvider().digest(blobObj.toProto().toByteArray())
        val receipt = transferReceiptService.encryptReceipt(
            TransferReceipt(uuid, "test_user", blobPriMultihash, AritegObjectType.BLOB)
        )

        val request = ReadObjectRequest.newBuilder()
            .setTransferReceipt(receipt)
            .build()
        val response = StreamRecorder.create<ValidateObjectResponse>()
        transferController.validateObject(request, response)
        assertNull(response.error)
        assertEquals(1, response.values.size)
        val result = response.values[0]
        assertEquals(blobPriMultihash, result.primaryHash.toMultihash())
    }

    @WithMockUser(username = "test_user", roles = ["VIEWER"])
    @Test
    fun cherryPick() {
        val uuid1 = fileRecordService.createRecord("test cherry pick 1", "test_user")
        val uuid2 = fileRecordService.createRecord("test cherry pick 2", "test_user")
        val blobContent = ByteString.copyFrom(Random.nextBytes(256))
        val blobObj = BlobObject(blobContent)
        aritegService.writeProto("", blobObj).completionFuture.get()
        val blobPriMultihash = getPrimaryMultihashProvider().digest(blobObj.toProto().toByteArray())
        val receipt = transferReceiptService.encryptReceipt(
            TransferReceipt(uuid1, "test_user", blobPriMultihash, AritegObjectType.BLOB)
        )

        val request = CherryPickRequest.newBuilder()
            .setCurrentTransferReceipt(receipt)
            .setTargetRecordUuid(uuid2.toString())
            .build()
        val response = StreamRecorder.create<TransferReceiptResponse>()
        transferController.cherryPick(request, response)
        assertNull(response.error)
        assertEquals(1, response.values.size)
        val result = response.values[0]
        assertEquals(blobPriMultihash, result.primaryHash.toMultihash())
        transferReceiptService.decryptReceipt(result.transferReceipt).let {
            assertNotNull(it)
            assertEquals(uuid2, it!!.recordId)
            assertEquals("test_user", it.username)
            assertEquals(blobPriMultihash, it.primaryHash)
        }
    }
}
