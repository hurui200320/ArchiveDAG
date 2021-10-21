package info.skyblond.archivedag.service.intf;

import info.skyblond.archivedag.model.bo.ProtoStatus;
import info.skyblond.archivedag.model.bo.RestoreOption;
import info.skyblond.archivedag.model.bo.StoreReceipt;
import info.skyblond.archivedag.model.exception.LoadProtoException;
import info.skyblond.archivedag.model.exception.ObjectRestorationException;
import info.skyblond.archivedag.model.exception.StoreProtoException;
import info.skyblond.ariteg.AritegLink;
import info.skyblond.ariteg.AritegObject;
import io.ipfs.multihash.Multihash;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;

public interface ProtoStorageService {
    /**
     * Store a proto to the storage, return a link with the given name.
     * <p>
     * Storing existing proto should have no bad side effects, since the proto
     * is immutable.
     */
    StoreReceipt storeProto(
            String name, AritegObject proto,
            BiFunction<Multihash, Multihash, Boolean> checkBeforeWrite
    ) throws StoreProtoException;

    /**
     * Fetch the status of the target proto.
     */
    ProtoStatus headLink(AritegLink link);

    /**
     * Restore link for reading. Some storage system like AWS S3
     * need restore the object before reading (Glacier).
     * <p>
     * The [option] depends. AWS S3 might require a duration for how long
     * the copy stay, and some other system might have different option.
     * <p>
     * Might throw any exception if something goes wrong.
     */
    CompletableFuture<Void> restoreLink(AritegLink link, RestoreOption option) throws ObjectRestorationException;

    /**
     * Load a proto from a given link.
     */
    AritegObject loadProto(AritegLink link) throws LoadProtoException;

    /**
     * Delete proto. *This will cause huge problem if you delete proto while writing.*
     * <p>
     * Writing process wil likely reuse the proto on disk, if you delete that,
     * the client will assume it's on disk but actually not.
     * <p>
     * Use at your own risk.
     */
    @Deprecated
    boolean deleteProto(AritegLink link);

    /**
     * Return the primary multihash type.
     */
    Multihash.Type getPrimaryMultihashType();

    /**
     * Return the secondary multihash type.
     */
    Multihash.Type getSecondaryMultihashType();
}
