package info.skyblond.archivedag.service.impl;

import com.google.protobuf.ByteString;
import info.skyblond.archivedag.model.bo.*;
import info.skyblond.archivedag.model.entity.MetaEntity;
import info.skyblond.archivedag.model.exception.*;
import info.skyblond.archivedag.service.intf.ProtoMetaService;
import info.skyblond.archivedag.service.intf.ProtoService;
import info.skyblond.archivedag.service.intf.ProtoStorageService;
import info.skyblond.archivedag.util.MultihashUtils;
import info.skyblond.ariteg.AritegLink;
import info.skyblond.ariteg.AritegObject;
import info.skyblond.ariteg.CommitData;
import info.skyblond.ariteg.ObjectType;
import info.skyblond.ariteg.objects.BlobObject;
import info.skyblond.ariteg.objects.CommitObject;
import info.skyblond.ariteg.objects.ListObject;
import info.skyblond.ariteg.objects.TreeObject;
import io.ipfs.multihash.Multihash;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class ProtoServiceImpl implements ProtoService {
    private final Logger logger = LoggerFactory.getLogger(ProtoServiceImpl.class);
    private final ProtoMetaService metaService;
    private final ProtoStorageService protoStorage;

    public ProtoServiceImpl(ProtoMetaService metaService, ProtoStorageService protoStorage) {
        this.metaService = metaService;
        this.protoStorage = protoStorage;
    }

    private WriteReceipt writeProto(String name, AritegObject proto) throws StoreProtoException {
        StoreReceipt receipt = this.protoStorage.storeProto(name, proto, (primary, secondary) -> {
            // lock the primary
            this.metaService.lock(primary);
            String secondaryHashBase58 = secondary.toBase58();
            // try to save, return old one if exists
            MetaEntity oldEntry = this.metaService.writeNewEntity(MetaEntity.builder()
                    .primaryMultihashBase58(primary.toBase58())
                    .secondaryMultihashBase58(secondaryHashBase58)
                    .objType(proto.getType().name())
                    .build());
            if (oldEntry != null) {
                this.logger.debug("Skip writing {}", primary.toBase58());
                // old one already exists
                if (!secondaryHashBase58.equals(oldEntry.getSecondaryMultihashBase58())) {
                    // if secondary hash not same, then report hash collision
                    throw new MultihashNotMatchError(secondaryHashBase58, oldEntry.getPrimaryMultihashBase58());
                }
                // release lock and cancel current writing
                this.metaService.unlock(primary);
                return false;
            } else {
                this.logger.debug("Confirm writing {}", primary.toBase58());
                // newly created, we have the lock, we can write
                return true;
            }
        });
        CompletableFuture<Void> future = receipt.getCompletionFuture()
                .thenAccept(primary -> {
                    if (primary != null) {
                        this.logger.debug("Finish writing {}", primary.toBase58());
                        // unlock only when this writing op is done
                        this.metaService.unlock(primary);
                    }
                });
        return new WriteReceipt(receipt.getLink(), future);
    }

    @Override
    public WriteReceipt writeChunk(String name, String mediaType, InputStream inputStream, int blobSize, int listLength) throws WriteChunkException {
        List<CompletableFuture<Void>> futureList = new LinkedList<>();
        List<AritegLink> linkList = new LinkedList<>();
        byte[] buffer = new byte[blobSize];
        int actualCount;
        do {
            try {
                actualCount = inputStream.read(buffer);
            } catch (IOException e) {
                throw new WriteChunkException(e);
            }
            if (actualCount == -1 && linkList.size() == 0) {
                // write an empty block when input is empty
                actualCount = 0;
            }
            // if we got count = -1, skip
            if (actualCount >= 0) {
                AritegObject blobObject = AritegObject.newBuilder()
                        .setType(ObjectType.BLOB)
                        .setData(ByteString.copyFrom(buffer, 0, actualCount))
                        .build();
                try {
                    WriteReceipt receipt = this.writeProto("", blobObject);
                    linkList.add(receipt.getLink());
                    futureList.add(receipt.getCompletionFuture());
                } catch (StoreProtoException e) {
                    throw new WriteChunkException(e);
                }
            }
        } while (actualCount > 0);
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
            AritegObject listObject = AritegObject.newBuilder()
                    .setType(ObjectType.LIST)
                    .addAllLinks(list)
                    .build();
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
            Multihash multihash = MultihashUtils.getFromAritegLink(linkList.get(0));
            if (!this.metaService.updateMediaType(multihash, mediaType)) {
                logger.warn("Cannot update media type: {} {}", multihash.toBase58(), mediaType);
            }
        });
        return new WriteReceipt(
                linkList.get(0).toBuilder().setName(name).build(),
                future
        );
    }

    @Override
    public ReadReceipt readChunk(AritegLink link) throws ReadChunkException {
        if (link.getType() != ObjectType.BLOB
                && link.getType() != ObjectType.LIST) {
            throw new ReadChunkException(new IllegalArgumentException(
                    "Unsupported object type " + link.getType().name() + " ," +
                            "only BLOB or LIST is supported."
            ));
        }

        Multihash primary = MultihashUtils.getFromAritegLink(link);

        String mediaType = this.metaService.findType(primary).getMediaType();
        if (mediaType == null) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }

        AritegObject obj;
        try {
            obj = this.protoStorage.loadProto(link);
        } catch (LoadProtoException e) {
            throw new ReadChunkException(e);
        }

        if (link.getType() == ObjectType.BLOB) {
            return new ReadReceipt(
                    mediaType, new ByteArrayInputStream(obj.getData().toByteArray())
            );
        }

        // it's list obj
        InputStream resultStream = new InputStream() {
            // init with all links
            final List<AritegLink> linkList = new LinkedList<>(obj.getLinksList());
            AritegObject currentBlob = null;
            int pointerInBlob = 0;

            private void fetchNextBlob() {
                // make sure we have more links to read
                while (!this.linkList.isEmpty() && this.currentBlob == null) {
                    // fetch the first link
                    AritegLink link = this.linkList.remove(0);
                    AritegObject obj;
                    try {
                        obj = ProtoServiceImpl.this.protoStorage.loadProto(link);
                    } catch (LoadProtoException e) {
                        throw new RuntimeException(e);
                    }

                    if (obj.getType() == ObjectType.BLOB) {
                        // is blob, use it
                        this.currentBlob = obj;
                        this.pointerInBlob = 0;
                        break; // break the loop, we are done
                    } else {
                        // else, is list, add them to the list and try again
                        if (obj.getType() != ObjectType.LIST) {
                            throw new AssertionError("Unsupported object type: " + obj.getType().name());
                        }
                        this.linkList.addAll(0, obj.getLinksList());
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

    @Override
    public BlobObject readBlob(AritegLink link) throws ReadBlobException {
        try {
            AritegObject obj = this.protoStorage.loadProto(link);
            return BlobObject.Companion.fromProto(obj);
        } catch (Throwable t) {
            throw new ReadBlobException(t);
        }
    }

    @Override
    public ListObject readList(AritegLink link) throws ReadListException {
        try {
            AritegObject obj = this.protoStorage.loadProto(link);
            return ListObject.Companion.fromProto(obj);
        } catch (Throwable t) {
            throw new ReadListException(t);
        }
    }

    @Override
    public WriteReceipt writeTree(String name, List<AritegLink> links) throws WriteTreeException {
        try {
            return this.writeProto(name, new TreeObject(links).toProto());
        } catch (StoreProtoException e) {
            throw new WriteTreeException(e);
        }
    }

    @Override
    public TreeObject readTree(AritegLink link) throws ReadTreeException {
        try {
            AritegObject obj = this.protoStorage.loadProto(link);
            return TreeObject.Companion.fromProto(obj);
        } catch (Throwable t) {
            throw new ReadTreeException(t);
        }
    }

    @Override
    public WriteReceipt writeCommit(String name, long unixTimestamp, String commitMessage, AritegLink parentLink, AritegLink commitContentLink, AritegLink authorLink) throws WriteCommitException {
        try {
            return this.writeProto(
                    name, new CommitObject(
                            unixTimestamp, commitMessage, parentLink,
                            commitContentLink, authorLink
                    ).toProto()
            );
        } catch (StoreProtoException e) {
            throw new WriteCommitException(e);
        }
    }

    @Override
    public CommitObject readCommit(AritegLink link) throws ReadCommitException {
        try {
            AritegObject obj = this.protoStorage.loadProto(link);
            return CommitObject.Companion.fromProto(obj);
        } catch (Throwable t) {
            throw new ReadCommitException(t);
        }
    }

    private List<AritegLink> resolveLinks(AritegLink link) throws ObjectRestorationException {
        List<AritegLink> result = new LinkedList<>();
        // add the link itself
        result.add(link);

        switch (link.getType()) {
            case BLOB:
                // nothing to add
                return result;
            case LIST:
            case TREE:
                try { // add all sub links
                    AritegObject obj = this.protoStorage.loadProto(link);
                    for (AritegLink aritegLink : obj.getLinksList()) {
                        if (aritegLink.getType() == ObjectType.BLOB) {
                            // add blob without resolve again
                            result.add(aritegLink);
                        } else {
                            // resolve
                            result.addAll(this.resolveLinks(aritegLink));
                        }
                    }
                } catch (LoadProtoException e) {
                    throw new ObjectRestorationException(e);
                }
                return result;
            case COMMIT:
                try { // add parent, author, and commit target
                    CommitData commit = this.protoStorage.loadProto(link).getCommitData();
                    result.addAll(this.resolveLinks(commit.getAuthor()));
                    // Restore current version only
//                    if (!commit.getParent().getMultihash().isEmpty()) {
//                        result.addAll(this.resolveLinks(commit.getParent()));
//                    }
                    result.addAll(this.resolveLinks(commit.getCommittedObject()));
                } catch (LoadProtoException e) {
                    throw new ObjectRestorationException(e);
                }
                return result;
            default:
                return List.of();
        }
    }

    @Override
    public RestoreReceipt restore(AritegLink link, RestoreOption option) throws ObjectRestorationException {
        List<AritegLink> links = this.resolveLinks(link);
        List<CompletableFuture<Void>> futures = new LinkedList<>();
        for (AritegLink aritegLink : links) {
            futures.add(this.protoStorage.restoreLink(aritegLink, option));
        }
        CompletableFuture<Void> future = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
        return new RestoreReceipt(links, future);
    }

    @Override
    public ProbeReceipt probe(Multihash primary) throws ObjectProbingException {
        FindTypeReceipt type = this.metaService.findType(primary);
        if (type == null) {
            return null;
        }
        AritegLink link = AritegLink.newBuilder()
                .setType(type.getObjectType())
                .setMultihash(ByteString.copyFrom(primary.toBytes()))
                .build();
        ProtoStatus status = this.protoStorage.headLink(link);
        String mediaType = type.getMediaType();
        return new ProbeReceipt(link, mediaType, status);
    }
}
