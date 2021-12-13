package info.skyblond.archivedag.service.impl;

import com.google.protobuf.ByteString;
import info.skyblond.archivedag.model.*;
import info.skyblond.archivedag.model.bo.*;
import info.skyblond.ariteg.chunking.ChunkProvider;
import info.skyblond.ariteg.chunking.ChunkProviderFactory;
import info.skyblond.ariteg.model.*;
import info.skyblond.ariteg.protos.AritegLink;
import info.skyblond.ariteg.protos.AritegObjectType;
import info.skyblond.ariteg.service.intf.AritegStorageService;
import io.ipfs.multihash.Multihash;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
public class AritegService {
    private final AritegMetaService metaService;
    private final AritegStorageService storageService;
    private final ChunkProviderFactory chunkProviderFactory;

    public AritegService(AritegMetaService metaService, AritegStorageService storageService, ChunkProviderFactory chunkProviderFactory) {
        this.metaService = metaService;
        this.storageService = storageService;
        this.chunkProviderFactory = chunkProviderFactory;
    }

    private WriteReceipt writeProto(String name, AritegObject proto) throws StoreProtoException {
        StoreReceipt receipt = this.storageService.store(name, proto, (primary, secondary) -> {
            // lock the primary
            this.metaService.lock(primary);
            if (this.metaService.createNewEntity(primary, secondary, proto.getObjectType())) {
                // get true -> this is a new proto
                log.debug("Confirm writing {}", primary.toBase58());
                // newly created, keep locking and confirm writing
                return true;
            } else {
                // get false -> the primary hash exists
                // check storage, make sure the proto is there
                if (storageService.queryStatus(AritegObjects.newLink(primary, proto.getObjectType())) == null) {
                    log.warn("Proto {} is missing!", primary.toBase58());
                    // keep locking and confirm writing
                    return true;
                } else {
                    log.debug("Skip writing {}", primary.toBase58());
                    // release lock and cancel current writing
                    this.metaService.unlock(primary);
                    return false;
                }
            }
        });
        CompletableFuture<Void> future = receipt.getCompletionFuture()
                .thenAccept(primary -> {
                    if (primary != null) {
                        log.debug("Finish writing {}", primary.toBase58());
                        // unlock only when this writing op is done
                        this.metaService.unlock(primary);
                    }
                });
        return new WriteReceipt(receipt.getLink(), future);
    }

    /**
     * Write a chunk of data (the input stream) into the system.
     * The data will be sliced according to blobSize and listLength.
     * The mediaType is only recorded to the root link.
     * The root link and a completable future will be returned.
     */
    public WriteReceipt writeChunk(String name, String mediaType, InputStream inputStream, int listLength) throws WriteChunkException {
        List<CompletableFuture<Void>> futureList = new LinkedList<>();
        List<AritegLink> linkList = new LinkedList<>();

        // slice and write blob
        ChunkProvider chunkProvider = chunkProviderFactory.newInstance(inputStream);
        ByteString chunk = chunkProvider.nextChunk();
        while (!chunk.isEmpty()) {
            BlobObject blob = new BlobObject(chunk);
            try {
                WriteReceipt receipt = this.writeProto("", blob);
                linkList.add(receipt.getLink());
                futureList.add(receipt.getCompletionFuture());
            } catch (StoreProtoException e) {
                throw new WriteChunkException(e);
            }
            chunk = chunkProvider.nextChunk();
        }

        // package links to listObj
        int i = 0;
        while (linkList.size() > 1) {
            // get some link
            int listSize = Math.min(listLength, linkList.size() - i);
            List<AritegLink> list = new LinkedList<>();
            for (int j = 0; j < listSize; j++) {
                list.add(linkList.remove(i));
            }
            // make it a list
            ListObject listObject = new ListObject(list);
            try {
                WriteReceipt receipt = this.writeProto("", listObject);
                // update i (next fetch point at next iter)
                linkList.add(i++, receipt.getLink());
                futureList.add(receipt.getCompletionFuture());
            } catch (StoreProtoException e) {
                throw new WriteChunkException(e);
            }
            // reset i if remained links are not enough for a new list
            if (i + listLength > linkList.size()) {
                i = 0;
            }
        }
        CompletableFuture<Void> future = CompletableFuture.allOf(
                futureList.toArray(new CompletableFuture[0])
        ).thenAccept(v -> {
            // update media type
            Multihash multihash = AritegObjects.extractMultihashFromLink(linkList.get(0));
            if (!this.metaService.updateMediaType(multihash, mediaType)) {
                log.warn("Cannot update media type: {} {}", multihash.toBase58(), mediaType);
            }
        });
        return new WriteReceipt(
                linkList.get(0).toBuilder().setName(name).build(),
                future
        );
    }

    /**
     * Read a chunk of data from a blob of list.
     * Return the mediaType and the input stream representing the data.
     */
    public ReadReceipt readChunk(AritegLink link) throws ReadChunkException {
        if (link.getType() != AritegObjectType.BLOB
                && link.getType() != AritegObjectType.LIST) {
            throw new ReadChunkException(new IllegalArgumentException(
                    "Unsupported object type " + link.getType().name() + " ," +
                            "only BLOB or LIST is supported."
            ));
        }

        Multihash primary = AritegObjects.extractMultihashFromLink(link);

        String mediaType = this.metaService.findType(primary).getMediaType();
        if (mediaType == null) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }

        AritegObject obj;
        try {
            obj = this.storageService.loadProto(link);
        } catch (LoadProtoException e) {
            throw new ReadChunkException(e);
        }

        // if the link is Blob, then return the content
        if (link.getType() == AritegObjectType.BLOB) {
            return new ReadReceipt(
                    mediaType, new ByteArrayInputStream(((BlobObject) obj).getData().toByteArray())
            );
        }

        // Otherwise, it's List
        InputStream resultStream = new InputStream() {
            // init with all links
            final List<AritegLink> linkList = new LinkedList<>(((ListObject) obj).getList());
            BlobObject currentBlob = null;
            int pointerInBlob = 0;

            private void fetchNextBlob() {
                // make sure we have more links to read
                while (!this.linkList.isEmpty() && this.currentBlob == null) {
                    // fetch the first link and read it
                    AritegLink link = this.linkList.remove(0);
                    AritegObject obj;
                    try {
                        obj = storageService.loadProto(link);
                    } catch (LoadProtoException e) {
                        throw new RuntimeException(e);
                    }

                    if (link.getType() == AritegObjectType.BLOB) {
                        // is blob, read and use it
                        this.currentBlob = (BlobObject) obj;
                        this.pointerInBlob = 0;
                        break; // break the loop, we are done
                    } else {
                        // else, assume is list, add them to the list and try again
                        this.linkList.addAll(0, ((ListObject) obj).getList());
                    }
                }
            }

            @Override
            public int read() {
                if (this.currentBlob == null) {
                    do { // refresh blob
                        this.fetchNextBlob();
                    } while (this.currentBlob != null && this.currentBlob.getData().size() == 0);
                    // if we get empty blob, skip it and fetch next
                    // stop when getting null blob
                }
                if (this.currentBlob == null) {
                    // really the end
                    return -1;
                }
                // we got the blob, read the value
                byte b = this.currentBlob.getData().byteAt(this.pointerInBlob++);
                int result = b & 0xff;
                if (this.pointerInBlob >= this.currentBlob.getData().size()) {
                    // if we are the end of blob, release it
                    this.currentBlob = null;
                }
                return result;
            }
        };
        return new ReadReceipt(mediaType, resultStream);
    }

    public BlobObject readBlob(AritegLink link) throws ReadBlobException {
        try {
            return (BlobObject) this.storageService.loadProto(link);
        } catch (Throwable t) {
            throw new ReadBlobException(t);
        }
    }

    public ListObject readList(AritegLink link) throws ReadListException {
        try {
            return (ListObject) this.storageService.loadProto(link);
        } catch (Throwable t) {
            throw new ReadListException(t);
        }
    }

    /**
     * Pack a list of link into a tree object.
     */
    public WriteReceipt writeTree(String name, List<AritegLink> links) throws WriteTreeException {
        try {
            return this.writeProto(name, new TreeObject(links));
        } catch (StoreProtoException e) {
            throw new WriteTreeException(e);
        }
    }

    public TreeObject readTree(AritegLink link) throws ReadTreeException {
        try {
            return (TreeObject) this.storageService.loadProto(link);
        } catch (Throwable t) {
            throw new ReadTreeException(t);
        }
    }

    /**
     * Make a commit object with a given link
     */
    public WriteReceipt writeCommit(String name, long unixTimestamp, String commitMessage, AritegLink parentLink, AritegLink commitContentLink, AritegLink authorLink) throws WriteCommitException {
        try {
            return this.writeProto(
                    name, new CommitObject(
                            unixTimestamp, commitMessage, parentLink,
                            commitContentLink, authorLink
                    )
            );
        } catch (StoreProtoException e) {
            throw new WriteCommitException(e);
        }
    }

    public CommitObject readCommit(AritegLink link) throws ReadCommitException {
        try {
            return (CommitObject) this.storageService.loadProto(link);
        } catch (Throwable t) {
            throw new ReadCommitException(t);
        }
    }

    public AritegLink renameLink(AritegLink link, String newName) {
        return link.toBuilder().setName(newName).build();
    }

    public void deleteLink(AritegLink link) {
        metaService.deleteByPrimaryHash(AritegObjects.extractMultihashFromLink(link));
        storageService.deleteProto(link);
    }

    /**
     * Resolve all related links.
     *
     * @param fullCommit true if you want to resolve the history commits.
     */
    public List<AritegLink> resolveLinks(AritegLink link, boolean fullCommit) throws ObjectRestorationException {
        List<AritegLink> result = new LinkedList<>();
        // add the link itself
        result.add(link);

        switch (link.getType()) {
            case BLOB:
                // nothing to add
                return result;
            case LIST:
                try { // add all sub links
                    ListObject obj = (ListObject) this.storageService.loadProto(link);
                    for (AritegLink aritegLink : obj.getList()) {
                        if (aritegLink.getType() == AritegObjectType.BLOB) {
                            // add blob without resolve again
                            result.add(aritegLink);
                        } else {
                            // resolve
                            result.addAll(this.resolveLinks(aritegLink, fullCommit));
                        }
                    }
                } catch (LoadProtoException e) {
                    throw new ObjectRestorationException(e);
                }
                return result;
            case TREE:
                try { // add all sub links
                    TreeObject obj = (TreeObject) this.storageService.loadProto(link);
                    for (AritegLink aritegLink : obj.getLinks()) {
                        if (aritegLink.getType() == AritegObjectType.BLOB) {
                            // add blob without resolve again
                            result.add(aritegLink);
                        } else {
                            // resolve
                            result.addAll(this.resolveLinks(aritegLink, fullCommit));
                        }
                    }
                } catch (LoadProtoException e) {
                    throw new ObjectRestorationException(e);
                }
                return result;
            case COMMIT:
                try { // add parent, author, and commit target
                    CommitObject commit = (CommitObject) this.storageService.loadProto(link);
                    result.addAll(this.resolveLinks(commit.getAuthorLink(), fullCommit));
                    // Restore history version on condition
                    if (fullCommit && !commit.getParentLink().getMultihash().isEmpty()) {
                        result.addAll(this.resolveLinks(commit.getParentLink(), true));
                    }
                    result.addAll(this.resolveLinks(commit.getCommittedObjectLink(), fullCommit));
                } catch (LoadProtoException e) {
                    throw new ObjectRestorationException(e);
                }
                return result;
            default:
                return List.of();
        }
    }

    public List<AritegLink> resolveLinks(AritegLink link) throws ObjectRestorationException {
        return resolveLinks(link, false);
    }

    /**
     * Give a root link and resolve all related link using resolveLinks(link, false).
     */
    public RestoreReceipt restore(AritegLink link, RestoreOption option) throws ObjectRestorationException {
        List<AritegLink> links = this.resolveLinks(link);
        List<CompletableFuture<Void>> futures = new LinkedList<>();
        for (AritegLink aritegLink : links) {
            futures.add(this.storageService.restoreLink(aritegLink, option));
        }
        CompletableFuture<Void> future = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
        return new RestoreReceipt(links, future);
    }

    /**
     * Probe the status of a give primary hash.
     * <p>
     * Return null if not found.
     */
    public ProbeReceipt probe(Multihash primary) {
        FindTypeReceipt type = this.metaService.findType(primary);
        if (type == null) {
            return null;
        }
        AritegLink link = AritegObjects.newLink(primary, type.getObjectType());
        StorageStatus status = this.storageService.queryStatus(link);
        if (status == null) {
            log.error("Link {} found in meta but not in storage", primary.toBase58());
            return null;
        }
        String mediaType = type.getMediaType();
        return new ProbeReceipt(link, mediaType, status);
    }
}
