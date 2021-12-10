package info.skyblond.ariteg.service.intf

import info.skyblond.archivedag.model.LoadProtoException
import info.skyblond.archivedag.model.ObjectRestorationException
import info.skyblond.archivedag.model.StoreProtoException
import info.skyblond.ariteg.model.*
import info.skyblond.ariteg.protos.AritegLink
import info.skyblond.ariteg.protos.AritegObjectType
import io.ipfs.multihash.Multihash
import java.util.concurrent.CompletableFuture
import java.util.function.BiFunction


/**
 * This is the service definition for storage.
 * It defines the minimal operations to read/write/query protos.
 * It doesn't care why or when should write or no.
 * */
interface AritegStorageService {
    /**
     * Store a proto to the storage and return a link with the given name.
     *
     * Storing same thing should have no side effects.
     * @return Pair<Link to this proto, future for this write task>
     *     null if checkBeforeWrite return false.
     * */
    @Throws(StoreProtoException::class)
    fun store(
        name: String, blob: BlobObject,
        checkBeforeWrite: BiFunction<Multihash, Multihash, Boolean>
    ): StoreReceipt

    @Throws(StoreProtoException::class)
    fun store(
        name: String, list: ListObject,
        checkBeforeWrite: BiFunction<Multihash, Multihash, Boolean>
    ): StoreReceipt

    @Throws(StoreProtoException::class)
    fun store(
        name: String, tree: TreeObject,
        checkBeforeWrite: BiFunction<Multihash, Multihash, Boolean>
    ): StoreReceipt

    @Throws(StoreProtoException::class)
    fun store(
        name: String, commitObject: CommitObject,
        checkBeforeWrite: BiFunction<Multihash, Multihash, Boolean>
    ): StoreReceipt

    /**
     * Fetch the status of the target proto.
     * Return null if the target proto not found.
     */
    fun queryStatus(link: AritegLink): StorageStatus?

    /**
     * Restore link for reading. Some storage system like AWS S3
     * need restore the object before reading (Glacier).
     *
     * The [option] depends. AWS S3 might require a duration for how long
     * the copy stay, and some other system might have different option.
     * Might be null.
     *
     * Might throw any exception if something goes wrong.
     *
     * Return a [CompletableFuture] represented if the request has been submitted.
     */
    @Throws(ObjectRestorationException::class)
    fun restoreLink(link: AritegLink, option: RestoreOption?): CompletableFuture<Void>

    /**
     * Load a proto from a given link.
     *
     * Return:
     *   NULL = null, no proto loaded
     *   BLOB = [BlobObject]
     *   LIST = [ListObject]
     *   TREE = [TreeObject]
     *   COMMIT = [CommitObject]
     */
    @Throws(LoadProtoException::class)
    fun loadProto(link: AritegLink): Pair<AritegObjectType, Any?>

    /**
     * Delete proto. Return true means deleted.
     *
     * Writing process wil likely reuse the proto on disk, if you delete that,
     * the client will assume it's on disk but actually not.
     *
     * Use at your own risk.
     */
    fun deleteProto(link: AritegLink): Boolean

    /**
     * Return the primary multihash type.
     */
    fun getPrimaryMultihashType(): Multihash.Type

    /**
     * Return the secondary multihash type.
     */
    fun getSecondaryMultihashType(): Multihash.Type
}
