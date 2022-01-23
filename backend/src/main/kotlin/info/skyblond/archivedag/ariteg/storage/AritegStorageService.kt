package info.skyblond.archivedag.ariteg.storage

import info.skyblond.archivedag.ariteg.model.*
import info.skyblond.archivedag.ariteg.protos.AritegLink
import io.ipfs.multihash.Multihash
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
    fun store(
        name: String, proto: AritegObject,
        checkBeforeWrite: BiFunction<Multihash, Multihash, Boolean>
    ): StoreReceipt

    /**
     * Fetch the status of the target proto.
     * Return null if the target proto not found.
     */
    fun queryStatus(link: AritegLink): StorageStatus?

    /**
     * Restore some links for reading. Some storage system like AWS S3
     * need restore the object before reading (Glacier).
     *
     * The [option] depends. AWS S3 might require a duration for how long
     * the copy stay, and some other system might have different option.
     * Might be null.
     *
     * Might throw any exception if something goes wrong.
     */
    fun restoreLink(link: AritegLink, option: RestoreOption?)

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
    fun loadProto(link: AritegLink): AritegObject

    /**
     * Delete proto. Return true means deleted.
     *
     * Writing process wil likely reuse the proto on disk, if you delete that,
     * the client will assume it's on disk but actually not.
     *
     * Use at your own risk.
     */
    fun deleteProto(link: AritegLink): Boolean

    fun primaryMultihashType(): Multihash.Type
    fun secondaryMultihashType(): Multihash.Type
}
