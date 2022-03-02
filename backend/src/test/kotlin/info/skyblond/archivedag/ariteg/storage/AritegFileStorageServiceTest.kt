package info.skyblond.archivedag.ariteg.storage

import com.google.protobuf.ByteString
import info.skyblond.archivedag.ariteg.model.BlobObject
import info.skyblond.archivedag.ariteg.model.CommitObject
import info.skyblond.archivedag.ariteg.model.ListObject
import info.skyblond.archivedag.ariteg.model.TreeObject
import info.skyblond.archivedag.ariteg.protos.AritegLink
import io.ipfs.multihash.Multihash
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import java.util.concurrent.CompletableFuture
import kotlin.random.Random

internal class AritegFileStorageServiceTest {

    private val baseDir = File("./data/test/${Random.nextLong()}")
    private val storageService = AritegFileStorageService(
        Multihash.Type.sha3_512, Multihash.Type.blake2b_512, baseDir, 2, 1024
    )

    @BeforeEach
    internal fun setUp() {
        baseDir.mkdirs()
    }

    @AfterEach
    internal fun tearDown() {
        this.storageService.close()
        baseDir.deleteRecursively()
    }

    @Test
    fun testNormalStoreAndLoad() {
        val blobData = ByteString.copyFrom("Data", Charsets.UTF_8)
        val storeBlob = this.storageService.store(
            "blob", BlobObject(blobData)
        ) { _, _ -> true }
        val storeList = this.storageService.store(
            "list", ListObject(listOf(storeBlob.link))
        ) { _, _ -> true }
        val storeTree = this.storageService.store(
            "tree", TreeObject(listOf(storeBlob.link, storeList.link))
        ) { _, _ -> true }
        val storeCommit = this.storageService.store(
            "commit", CommitObject(
                1234, "message", storeBlob.link,
                storeList.link, storeTree.link
            )
        ) { _, _ -> true }
        CompletableFuture.allOf(
            storeBlob.completionFuture, storeList.completionFuture,
            storeTree.completionFuture, storeCommit.completionFuture
        ).get()

        assertEquals(
            blobData,
            (this.storageService.loadProto(storeBlob.link) as BlobObject).data
        )
        assertEquals(
            listOf(storeBlob.link),
            (this.storageService.loadProto(storeList.link) as ListObject).list
        )
        assertEquals(
            listOf(storeBlob.link, storeList.link),
            (this.storageService.loadProto(storeTree.link) as TreeObject).links
        )
        val commitObj = this.storageService.loadProto(storeCommit.link) as CommitObject
        assertEquals(1234, commitObj.unixTimestamp)
        assertEquals("message", commitObj.message)
        assertEquals(storeBlob.link, commitObj.parentLink)
        assertEquals(storeList.link, commitObj.committedObjectLink)
        assertEquals(storeTree.link, commitObj.authorLink)
    }

    @Test
    fun testCancelStore() {
        val storeBlob = this.storageService.store(
            "blob", BlobObject(ByteString.copyFrom("Data", Charsets.UTF_8))
        ) { _, _ -> false }
        assertNull(storeBlob.completionFuture.get())
        assertThrows(IllegalStateException::class.java) {
            this.storageService.loadProto(storeBlob.link)
        }
    }

    @Test
    fun deleteProto() {
        val blobData = ByteString.copyFrom("Data", Charsets.UTF_8)
        val storeBlob = this.storageService.store(
            "blob", BlobObject(blobData)
        ) { _, _ -> true }
        assertNotNull(storeBlob.completionFuture.get())
        assertEquals(
            blobData,
            (this.storageService.loadProto(storeBlob.link) as BlobObject).data
        )
        assertTrue(this.storageService.deleteProto(storeBlob.link))
        assertThrows(IllegalStateException::class.java) {
            this.storageService.loadProto(storeBlob.link)
        }
        assertFalse(this.storageService.deleteProto(storeBlob.link))
    }

    @Test
    fun queryStatus() {
        val storeBlob = this.storageService.store(
            "blob", BlobObject(ByteString.copyFrom("Data", Charsets.UTF_8))
        ) { _, _ -> true }
        assertNotNull(storeBlob.completionFuture.get())
        assertTrue(this.storageService.queryStatus(storeBlob.link)!!.available)
        assertNotEquals(0, this.storageService.queryStatus(storeBlob.link)!!.protoSize)

        assertTrue(this.storageService.deleteProto(storeBlob.link))
        assertNull(this.storageService.queryStatus(storeBlob.link))
    }

    @Test
    fun restoreLink() {
        assertDoesNotThrow {
            this.storageService.restoreLink(AritegLink.getDefaultInstance())
        }
    }
}
