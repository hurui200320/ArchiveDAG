package info.skyblond.archivedag.service.impl;

import info.skyblond.archivedag.config.EmbeddedRedisConfiguration;
import info.skyblond.archivedag.config.WebMvcConfig;
import info.skyblond.archivedag.model.*;
import info.skyblond.archivedag.model.bo.ReadReceipt;
import info.skyblond.archivedag.model.bo.RestoreReceipt;
import info.skyblond.archivedag.model.bo.WriteReceipt;
import info.skyblond.ariteg.chunking.ChunkProviderFactory;
import info.skyblond.ariteg.chunking.FixedLengthChunkProvider;
import info.skyblond.ariteg.model.*;
import info.skyblond.ariteg.multihash.MultihashProvider;
import info.skyblond.ariteg.multihash.MultihashProviders;
import info.skyblond.ariteg.protos.AritegLink;
import info.skyblond.ariteg.service.intf.AritegInMemoryStorageService;
import info.skyblond.ariteg.service.intf.AritegStorageService;
import io.ipfs.multihash.Multihash;
import kotlin.random.Random;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(
        classes = {EmbeddedRedisConfiguration.class, WebMvcConfig.class}
)
@ActiveProfiles("test")
class AritegServiceTest {

    @Autowired
    AritegMetaService metaService;

    MultihashProvider primary = MultihashProviders.fromMultihashType(Multihash.Type.sha3_512);
    MultihashProvider secondary = MultihashProviders.fromMultihashType(Multihash.Type.blake2b_512);
    AritegStorageService storageService = new AritegInMemoryStorageService(primary, secondary);
    int chunkSize = 512; // 512 byte per chunk
    ChunkProviderFactory chunkProviderFactory = inputStream -> new FixedLengthChunkProvider(inputStream, chunkSize);

    AritegService aritegService;

    @BeforeEach
    void setUp() {
        aritegService = new AritegService(metaService, storageService, chunkProviderFactory);
    }

    @Test
    void testRWBlob() throws WriteChunkException, ExecutionException, InterruptedException, ReadChunkException, IOException, ReadBlobException {
        // a blob
        byte[] content = new byte[chunkSize - 1];
        Random.Default.nextBytes(content);

        // write blob
        InputStream inputStream = new ByteArrayInputStream(content);
        WriteReceipt writeReceipt = aritegService.writeChunk(
                "name", MediaType.APPLICATION_OCTET_STREAM_VALUE,
                inputStream, 176);
        writeReceipt.getCompletionFuture().get();
        inputStream.close();
        // write again
        inputStream = new ByteArrayInputStream(content);
        writeReceipt = aritegService.writeChunk(
                "name", MediaType.APPLICATION_OCTET_STREAM_VALUE,
                inputStream, 176);
        writeReceipt.getCompletionFuture().get();
        inputStream.close();

        // read chunk
        ReadReceipt readReceipt = aritegService.readChunk(writeReceipt.getLink());
        assertEquals(MediaType.APPLICATION_OCTET_STREAM_VALUE, readReceipt.getMediaType());
        assertArrayEquals(content, readReceipt.getInputStream().readAllBytes());
        readReceipt.getInputStream().close();

        // read blob
        BlobObject blob = aritegService.readBlob(writeReceipt.getLink());
        assertArrayEquals(content, blob.getData().toByteArray());
    }

    @Test
    void testRWOneLayerList() throws WriteChunkException, ExecutionException, InterruptedException, ReadChunkException, IOException, ReadListException {
        // a list with 4 blob
        byte[] content = new byte[3 * chunkSize + 3];
        Random.Default.nextBytes(content);

        // write list
        InputStream inputStream = new ByteArrayInputStream(content);
        WriteReceipt writeReceipt = aritegService.writeChunk(
                "name", MediaType.APPLICATION_OCTET_STREAM_VALUE,
                inputStream, 176);
        writeReceipt.getCompletionFuture().get();
        inputStream.close();

        // read chunk
        ReadReceipt readReceipt = aritegService.readChunk(writeReceipt.getLink());
        assertEquals(MediaType.APPLICATION_OCTET_STREAM_VALUE, readReceipt.getMediaType());
        assertArrayEquals(content, readReceipt.getInputStream().readAllBytes());
        readReceipt.getInputStream().close();

        // read list
        ListObject listObj = aritegService.readList(writeReceipt.getLink());
        assertEquals(4, listObj.getList().size());
    }

    @Test
    void testRWMultiLayerList() throws WriteChunkException, ExecutionException, InterruptedException, ReadChunkException, IOException, ReadListException {
        // a list with 9 blob -> root -> (4) (4) 1
        byte[] content = new byte[8 * chunkSize + 3];
        Random.Default.nextBytes(content);

        // write list
        InputStream inputStream = new ByteArrayInputStream(content);
        WriteReceipt writeReceipt = aritegService.writeChunk(
                "name", MediaType.APPLICATION_OCTET_STREAM_VALUE,
                inputStream, 4);
        writeReceipt.getCompletionFuture().get();
        inputStream.close();

        // read chunk
        ReadReceipt readReceipt = aritegService.readChunk(writeReceipt.getLink());
        assertEquals(MediaType.APPLICATION_OCTET_STREAM_VALUE, readReceipt.getMediaType());
        assertArrayEquals(content, readReceipt.getInputStream().readAllBytes());
        readReceipt.getInputStream().close();

        // read list
        ListObject listObj = aritegService.readList(writeReceipt.getLink());
        assertEquals(3, listObj.getList().size());
        assertEquals(4, aritegService.readList(listObj.getList().get(0)).getList().size());
        assertEquals(4, aritegService.readList(listObj.getList().get(1)).getList().size());
    }

    private AritegLink writeBlob(String name) throws WriteChunkException, ExecutionException, InterruptedException, IOException {
        byte[] content = new byte[chunkSize - 1];
        Random.Default.nextBytes(content);

        // write blob
        InputStream inputStream = new ByteArrayInputStream(content);
        WriteReceipt writeReceipt = aritegService.writeChunk(
                name, MediaType.APPLICATION_OCTET_STREAM_VALUE,
                inputStream, 176);
        writeReceipt.getCompletionFuture().get();
        inputStream.close();
        return writeReceipt.getLink();
    }

    @Test
    void testRWTree() throws WriteChunkException, IOException, ExecutionException, InterruptedException, WriteTreeException, ReadTreeException {
        List<AritegLink> linkList = List.of(writeBlob("#1"), writeBlob("#2"), writeBlob("#3"));
        WriteReceipt writeReceipt = aritegService.writeTree("name", linkList);
        writeReceipt.getCompletionFuture().get();

        TreeObject tree = aritegService.readTree(writeReceipt.getLink());
        assertEquals(linkList, tree.getLinks());
    }

    @Test
    void testRWCommit() throws WriteChunkException, IOException, ExecutionException, InterruptedException, WriteCommitException, ReadCommitException {
        AritegLink parent = writeBlob("parent");
        AritegLink content = writeBlob("content");
        AritegLink author = writeBlob("author");
        WriteReceipt writeReceipt = aritegService.writeCommit(
                "name", 1234, "message",
                parent, content, author);
        writeReceipt.getCompletionFuture().get();

        CommitObject commit = aritegService.readCommit(writeReceipt.getLink());
        assertEquals(1234, commit.getUnixTimestamp());
        assertEquals("message", commit.getMessage());
        assertEquals(parent, commit.getParentLink());
        assertEquals(content, commit.getCommittedObjectLink());
        assertEquals(author, commit.getAuthorLink());
    }

    @Test
    void testRenameLink() throws WriteChunkException, IOException, ExecutionException, InterruptedException {
        AritegLink link = writeBlob("name");
        assertEquals("name", link.getName());
        link = aritegService.renameLink(link, "newName");
        assertEquals("newName", link.getName());
    }

    @Test
    void testDeleteLink() throws WriteChunkException, IOException, ExecutionException, InterruptedException {
        AritegLink link = writeBlob("");
        assertEquals(link, aritegService.probe(AritegObjects.extractMultihashFromLink(link)).getLink());
        assertDoesNotThrow(() -> aritegService.readBlob(link));

        aritegService.deleteLink(link);
        assertNull(aritegService.probe(AritegObjects.extractMultihashFromLink(link)));
        assertThrows(ReadBlobException.class, () -> aritegService.readBlob(link));
    }

    @Test
    void testResolveBlobLink() throws WriteChunkException, IOException, ExecutionException, InterruptedException, ObjectRestorationException {
        AritegLink link = writeBlob("self");
        List<AritegLink> result = aritegService.resolveLinks(link);
        assertEquals(1, result.size());
        assertEquals(link, result.get(0));
    }

    @Test
    void testResolveListLink() throws WriteChunkException, IOException, ExecutionException, InterruptedException, ObjectRestorationException {
        // a list with 9 blob -> root -> (4) (4) 1
        byte[] content = new byte[8 * chunkSize + 3];
        Random.Default.nextBytes(content);

        // write list
        InputStream inputStream = new ByteArrayInputStream(content);
        WriteReceipt writeReceipt = aritegService.writeChunk(
                "name", MediaType.APPLICATION_OCTET_STREAM_VALUE,
                inputStream, 4);
        writeReceipt.getCompletionFuture().get();
        inputStream.close();

        List<AritegLink> result = aritegService.resolveLinks(writeReceipt.getLink());
        assertEquals(12, result.size());
        assertEquals(writeReceipt.getLink(), result.get(0));
    }

    @Test
    void testResolveTreeLink() throws WriteChunkException, IOException, ExecutionException, InterruptedException, ObjectRestorationException, WriteTreeException {
        // a list with 4 blob
        byte[] content = new byte[3 * chunkSize + 3];
        Random.Default.nextBytes(content);

        // write list
        InputStream inputStream = new ByteArrayInputStream(content);
        WriteReceipt writeReceipt = aritegService.writeChunk(
                "name", MediaType.APPLICATION_OCTET_STREAM_VALUE,
                inputStream, 176);
        writeReceipt.getCompletionFuture().get();
        inputStream.close();

        List<AritegLink> linkList = List.of(writeBlob("#1"), writeBlob("#2"), writeReceipt.getLink());
        writeReceipt = aritegService.writeTree("name", linkList);
        writeReceipt.getCompletionFuture().get();

        List<AritegLink> result = aritegService.resolveLinks(writeReceipt.getLink());
        assertEquals(8, result.size());
        assertEquals(writeReceipt.getLink(), result.get(0));
    }

    @Test
    void testResolveCommitWithoutHistory() throws WriteChunkException, IOException, ExecutionException, InterruptedException, WriteCommitException, ObjectRestorationException {
        // commit #1
        AritegLink content = writeBlob("content");
        AritegLink author = writeBlob("author");
        WriteReceipt writeReceipt = aritegService.writeCommit(
                "name", 1234, "message",
                AritegLink.getDefaultInstance(), content, author);
        writeReceipt.getCompletionFuture().get();
        // commit #2 -> #1
        content = writeBlob("content");
        author = writeBlob("author");
        writeReceipt = aritegService.writeCommit(
                "name", 1234, "message",
                writeReceipt.getLink(), content, author);
        writeReceipt.getCompletionFuture().get();

        List<AritegLink> result = aritegService.resolveLinks(writeReceipt.getLink(), false);
        assertEquals(3, result.size());
        assertEquals(writeReceipt.getLink(), result.get(0));
    }

    @Test
    void testResolveCommitWithHistory() throws WriteChunkException, IOException, ExecutionException, InterruptedException, WriteCommitException, ObjectRestorationException {
        // commit #1
        AritegLink content = writeBlob("content");
        AritegLink author = writeBlob("author");
        WriteReceipt writeReceipt = aritegService.writeCommit(
                "name", 1234, "message",
                AritegLink.getDefaultInstance(), content, author);
        writeReceipt.getCompletionFuture().get();
        // commit #2 -> #1
        content = writeBlob("content");
        author = writeBlob("author");
        writeReceipt = aritegService.writeCommit(
                "name", 1234, "message",
                writeReceipt.getLink(), content, author);
        writeReceipt.getCompletionFuture().get();

        List<AritegLink> result = aritegService.resolveLinks(writeReceipt.getLink(), true);
        assertEquals(6, result.size());
        assertEquals(writeReceipt.getLink(), result.get(0));
    }

    @Test
    void testRestore() throws WriteChunkException, IOException, ExecutionException, InterruptedException, ObjectRestorationException {
        AritegLink link = writeBlob("name");
        RestoreReceipt receipt = aritegService.restore(link, null);
        receipt.getCompletionFuture().get();
        assertEquals(aritegService.resolveLinks(link), receipt.getInvolvedLinks());
    }
}
