package info.skyblond.archivedag.ariteg

import com.google.protobuf.ByteString
import info.skyblond.archivedag.ariteg.model.*
import info.skyblond.archivedag.ariteg.multihash.MultihashProviders
import info.skyblond.archivedag.ariteg.protos.AritegLink
import info.skyblond.archivedag.ariteg.protos.AritegObjectType
import info.skyblond.archivedag.ariteg.service.AritegMetaService
import info.skyblond.archivedag.ariteg.service.DistributedLockService
import info.skyblond.archivedag.ariteg.storage.AritegInMemoryStorageService
import info.skyblond.archivedag.ariteg.storage.AritegStorageService
import info.skyblond.archivedag.ariteg.utils.toMultihash
import io.ipfs.multihash.Multihash
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import kotlin.random.Random

/**
 * This is a full test of ariteg package, it involves:
 * + [AritegService]
 * + [AritegMetaService]
 * + [AritegStorageService] (Using [AritegInMemoryStorageService])
 * + Multihash packages (has individual test code)
 * + Models
 *
 * But the configs are not tested.
 * */
@SpringBootTest
@ActiveProfiles("test")
internal class AritegServiceTest {
    @Autowired
    lateinit var metaService: AritegMetaService

    @Autowired
    lateinit var lockService: DistributedLockService

    private final val primary = MultihashProviders.fromMultihashType(Multihash.Type.sha3_512)
    private final val secondary = MultihashProviders.fromMultihashType(Multihash.Type.blake2b_512)
    val storageService: AritegStorageService = AritegInMemoryStorageService(primary, secondary)
    lateinit var aritegService: AritegService

    private val chunkSize = 256 // 256 Bytes for a chunk

    @BeforeEach
    fun setUp() {
        aritegService = AritegService(metaService, storageService, lockService)
    }

    @Test
    fun testResolveCommitWithHistory() {
        val commit1Link = writeCommit("#1", getEmptyCommitLink())
        val commit2Link = writeCommit("#2", commit1Link)
        // commit #2 -> #1

        val result: List<AritegLink> = aritegService.resolveLinks(commit2Link, true)
        // #2, content, author, #1, content, author
        assertEquals(6, result.size)
        assertEquals(commit2Link, result[0])
    }

    @Test
    fun testRestore() {
        val link = writeBlob("name").first
        val (involvedLinks) = aritegService.restore(link, null)
        assertEquals(aritegService.resolveLinks(link), involvedLinks)
    }

    @Test
    fun testResolveCommitWithoutHistory() {
        val commit1Link = writeCommit("#1", getEmptyCommitLink())
        val commit2Link = writeCommit("#2", commit1Link)
        // commit #2 -> #1

        val result: List<AritegLink> = aritegService.resolveLinks(commit2Link, false)
        // self, content, author
        assertEquals(3, result.size)
        assertEquals(commit2Link, result[0])
    }

    private fun getEmptyCommitLink(): AritegLink {
        return AritegLink.newBuilder().setType(AritegObjectType.COMMIT).build()
    }

    private fun writeCommit(name: String, parent: AritegLink): AritegLink {
        val content = writeBlob("content").first
        val author = writeBlob("author").first
        val writeReceipt: WriteReceipt = aritegService.writeProto(
            name, CommitObject(
                unixTimestamp = 1234,
                message = "message",
                parentLink = parent,
                committedObjectLink = content,
                authorLink = author
            )
        )
        writeReceipt.completionFuture.get()
        return writeReceipt.link
    }

    @Test
    fun testResolveTreeLink() {
        // Tree: L(B,B,B,B), L(B,B,B,B), B, B
        val lists = listOf(
            writeList("#1"), writeList("#2"),
            writeBlob("#3"), writeBlob("#4")
        )
        val linkList = lists.map { it.first }

        // write list
        val writeReceipt = aritegService.writeProto(
            "name", TreeObject(linkList)
        )
        writeReceipt.completionFuture.get()

        val result: List<AritegLink> = aritegService.resolveLinks(writeReceipt.link)
        assertEquals(13, result.size)
        assertEquals(writeReceipt.link, result[0])
    }

    @Test
    fun testResolveListLink() {
        // List: L(B,B,B,B), L(B,B,B,B), B, B
        val lists = listOf(
            writeList(""), writeList(""),
            writeBlob(""), writeBlob("")
        )
        val linkList = lists.map { it.first }

        // write list
        val writeReceipt = aritegService.writeProto(
            "name", ListObject(linkList)
        )
        writeReceipt.completionFuture.get()

        val result: List<AritegLink> = aritegService.resolveLinks(writeReceipt.link)
        assertEquals(13, result.size)
        // the first one is self ref
        assertEquals(writeReceipt.link, result[0])
    }

    @Test
    fun testResolveBlobLink() {
        val link = writeBlob("self").first
        val result: List<AritegLink> = aritegService.resolveLinks(link)
        assertEquals(1, result.size)
        assertEquals(link, result[0])
    }

    @Test
    fun testDeleteLink() {
        val link = writeBlob("").first
        assertEquals(link, aritegService.probe(link.multihash.toMultihash())!!.link)
        assertDoesNotThrow { aritegService.readBlob(link) }
        aritegService.deleteLink(link)
        assertNull(aritegService.probe(link.multihash.toMultihash()))
        assertThrows(IllegalStateException::class.java) { aritegService.readBlob(link) }
    }

    @Test
    fun testRenameLink() {
        var link = writeBlob("name").first
        assertEquals("name", link.name)
        link = aritegService.renameLink(link, "newName")
        assertEquals("newName", link.name)
    }

    @Test
    fun testRWCommit() {
        val parent = getEmptyCommitLink()
        val content = writeBlob("content").first
        val author = writeBlob("author").first
        val (link, completionFuture) = aritegService.writeProto(
            "name", CommitObject(
                unixTimestamp = 1234,
                message = "message",
                parentLink = parent,
                committedObjectLink = content,
                authorLink = author
            )
        )
        completionFuture.get()
        val (unixTimestamp, message, parentLink, committedObjectLink, authorLink) = aritegService.readCommit(link)
        assertEquals(1234, unixTimestamp)
        assertEquals("message", message)
        assertEquals(parent, parentLink)
        assertEquals(content, committedObjectLink)
        assertEquals(author, authorLink)
    }

    @Test
    fun testRWTree() {
        val linkList = listOf(
            writeBlob("#1"), writeBlob("#2"), writeBlob("#3")
        ).map { it.first }
        val (link, completionFuture) = aritegService.writeProto("name", TreeObject(linkList))
        completionFuture.get()
        val (links) = aritegService.readTree(link)
        assertEquals(linkList, links)
    }

    private fun writeList(name: String): Pair<AritegLink, ListObject> {
        val blobs = Array(4) { writeBlob("") }
        val listObject = ListObject(blobs.map { it.first })
        val writeReceipt = aritegService.writeProto(name, listObject)
        writeReceipt.completionFuture.get()
        return writeReceipt.link to listObject
    }

    @Test
    fun testRWOneLayerList() {
        // a list with 4 blob
        val blobs = Array(4) { writeBlob("") }
        val linkList = blobs.map { it.first }

        // write list
        val writeReceipt = aritegService.writeProto(
            "name", ListObject(linkList)
        )
        writeReceipt.completionFuture.get()

        // read list
        val (actualList) = aritegService.readList(writeReceipt.link)
        assertEquals(linkList, actualList)
    }

    private fun writeBlob(name: String): Pair<AritegLink, BlobObject> {
        val content = ByteArray(chunkSize)
        Random.nextBytes(content)
        val blobObject = BlobObject(ByteString.copyFrom(content))
        val writeReceipt = aritegService.writeProto(name, blobObject)
        writeReceipt.completionFuture.get()
        return writeReceipt.link to blobObject
    }

    @Test
    fun testRWBlob() {
        // a blob
        val content = ByteArray(chunkSize)
        Random.nextBytes(content)

        // write a blob
        val writeReceipt = aritegService.writeProto(
            "name", BlobObject(ByteString.copyFrom(content))
        )
        writeReceipt.completionFuture.get()
        // write again
        val duplicatedWriteReceipt = aritegService.writeProto(
            "name", BlobObject(ByteString.copyFrom(content))
        )
        duplicatedWriteReceipt.completionFuture.get()
        // should have same link
        assertEquals(writeReceipt.link, duplicatedWriteReceipt.link)

        // read out content
        val blobObject = aritegService.readBlob(writeReceipt.link)
        assertArrayEquals(content, blobObject.data.toByteArray())
    }

    @Test
    fun testCheckCollision() {
        // test not found
        assertNull(
            aritegService.checkCollision(
                primary.digest("404".encodeToByteArray()),
                secondary.digest("404".encodeToByteArray())
            )
        )
        // test not collided
        val content = ByteArray(chunkSize)
        Random.nextBytes(content)
        val blobObject = BlobObject(ByteString.copyFrom(content))
        val writeReceipt = aritegService.writeProto("", blobObject)
        writeReceipt.completionFuture.get()
        val primaryMultihash = writeReceipt.link.multihash.toMultihash()
        assertEquals(primary.digest(blobObject.toProto().toByteArray()), primaryMultihash)
        assertFalse(
            aritegService.checkCollision(
                primaryMultihash, secondary.digest(blobObject.toProto().toByteArray())
            )!!
        )
        // test collided
        assertTrue(
            aritegService.checkCollision(
                primaryMultihash, secondary.digest(ByteArray(0))
            )!!
        )
    }

    @Test
    fun testUpdateMediaType() {
        val (link, _) = writeBlob("")
        val multihash = link.multihash.toMultihash()
        assertNull(aritegService.probe(multihash)!!.mediaType)
        aritegService.updateMediaType(link, "something")
        assertEquals("something", aritegService.probe(multihash)!!.mediaType)
    }
}
