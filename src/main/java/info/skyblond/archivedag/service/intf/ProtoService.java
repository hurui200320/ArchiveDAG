package info.skyblond.archivedag.service.intf;

import info.skyblond.archivedag.model.bo.*;
import info.skyblond.archivedag.model.exception.*;
import info.skyblond.ariteg.AritegLink;
import info.skyblond.ariteg.objects.CommitObject;
import info.skyblond.ariteg.objects.TreeObject;
import io.ipfs.multihash.Multihash;

import java.io.InputStream;
import java.util.List;

public interface ProtoService {
    /**
     * Write a chunk of data (the input stream) into the system.
     * The data will be sliced according to blobSize and listLength.
     * The mediaType is only recorded to the root link.
     * The root link and a completable future will be returned.
     */
    WriteReceipt writeChunk(
            String name, String mediaType,
            InputStream inputStream,
            int blobSize, int listLength
    ) throws WriteChunkException;

    /**
     * Read a chunk of data from a blob of list.
     * Return the mediaType and the input stream representing the data.
     */
    ReadReceipt readChunk(AritegLink link) throws ReadChunkException;

    /**
     * Pack a list of link into a tree object.
     */
    WriteReceipt writeTree(String name, List<AritegLink> links) throws WriteTreeException;

    TreeObject readTree(AritegLink link) throws ReadTreeException;

    /**
     * Make a commit object with a given link
     */
    WriteReceipt writeCommit(
            String name, long unixTimestamp, String commitMessage,
            AritegLink parentLink, AritegLink commitContentLink, AritegLink authorLink
    ) throws WriteCommitException;

    CommitObject readCommit(AritegLink link) throws ReadCommitException;

    RestoreReceipt restore(AritegLink link, RestoreOption option) throws ObjectRestorationException;

    ProbeReceipt probe(Multihash primary) throws ObjectProbingException;
}
