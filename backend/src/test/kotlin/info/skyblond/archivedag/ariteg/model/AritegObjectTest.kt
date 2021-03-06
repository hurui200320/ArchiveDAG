package info.skyblond.archivedag.ariteg.model

import com.google.protobuf.ByteString
import info.skyblond.archivedag.ariteg.multihash.MultihashProviders
import info.skyblond.archivedag.ariteg.protos.*
import io.ipfs.multihash.Multihash
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class AritegObjectTest {

    private val multihashProvider = MultihashProviders.fromMultihashType(Multihash.Type.blake2b_256)

    @Test
    fun testBlob() {
        val original = AritegBlobObject.newBuilder()
            .setData(ByteString.copyFrom("Some test data", Charsets.UTF_8))
            .build()
        val obj = AritegObjects.fromProto(original)
        assertEquals(original.data, obj.data)
        val recovered = obj.toProto()
        assertEquals(original.data, recovered.data)
    }

    @Test
    fun testList() {
        val original = AritegListObject.newBuilder()
            .addAllLinks(
                listOf(
                    AritegObjects.newLink(
                        multihashProvider.digest("multihash#1".encodeToByteArray()),
                        AritegObjectType.BLOB
                    ),
                    AritegObjects.newLink(
                        multihashProvider.digest("multihash#2".encodeToByteArray()),
                        AritegObjectType.LIST
                    )
                )
            )
            .build()
        val obj = AritegObjects.fromProto(original)
        assertEquals(original.linksList, obj.list)
        val recovered = obj.toProto()
        assertEquals(original.linksList, recovered.linksList)
    }

    @Test
    fun testTree() {
        val original = AritegTreeObject.newBuilder()
            .addAllLinks(
                listOf(
                    AritegObjects.newLink(
                        "name#1",
                        multihashProvider.digest("multihash#1".encodeToByteArray()),
                        AritegObjectType.BLOB
                    ),
                    AritegObjects.newLink(
                        "name#2",
                        multihashProvider.digest("multihash#2".encodeToByteArray()),
                        AritegObjectType.LIST
                    )
                )
            )
            .build()
        val obj = AritegObjects.fromProto(original)
        assertEquals(original.linksList, obj.links)
        val recovered = obj.toProto()
        assertEquals(original.linksList, recovered.linksList)
    }

    @Test
    fun testCommit() {
        val original = AritegCommitObject.newBuilder()
            .setUnixTimestamp(1234)
            .setMessage("Some message")
            .setParent(
                AritegObjects.newLink(
                    multihashProvider.digest("multihash#1".encodeToByteArray()),
                    AritegObjectType.BLOB
                )
            )
            .setCommittedObject(
                AritegObjects.newLink(
                    multihashProvider.digest("multihash#2".encodeToByteArray()),
                    AritegObjectType.LIST
                )
            )
            .setAuthor(
                AritegObjects.newLink(
                    multihashProvider.digest("multihash#3".encodeToByteArray()),
                    AritegObjectType.TREE
                )
            )
            .build()
        val obj = AritegObjects.fromProto(original)
        assertEquals(original.unixTimestamp, obj.unixTimestamp)
        assertEquals(original.message, obj.message)
        assertEquals(original.parent, obj.parentLink)
        assertEquals(original.committedObject, obj.committedObjectLink)
        assertEquals(original.author, obj.authorLink)
        val recovered = obj.toProto()
        assertEquals(original.unixTimestamp, recovered.unixTimestamp)
        assertEquals(original.message, recovered.message)
        assertEquals(original.parent, recovered.parent)
        assertEquals(original.committedObject, recovered.committedObject)
        assertEquals(original.author, recovered.author)
    }

    @Test
    fun testEmpty() {
        val tree = TreeObject(emptyList()).toProto().toByteArray()
        val blob = BlobObject(ByteString.EMPTY).toProto().toByteArray()
        assertArrayEquals(tree, blob)
    }

}
