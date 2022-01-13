package info.skyblond.archivedag.ariteg

import info.skyblond.archivedag.ariteg.model.*
import info.skyblond.archivedag.ariteg.model.AritegObjects.newLink
import info.skyblond.archivedag.ariteg.protos.AritegLink
import info.skyblond.archivedag.ariteg.protos.AritegObjectType
import info.skyblond.archivedag.ariteg.service.AritegMetaService
import info.skyblond.archivedag.ariteg.service.DistributedLockService
import info.skyblond.archivedag.ariteg.storage.AritegStorageService
import info.skyblond.archivedag.ariteg.utils.toMultihash
import info.skyblond.archivedag.commons.service.EtcdSimpleLock
import io.ipfs.multihash.Multihash
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.*
import java.util.concurrent.atomic.AtomicReference

@Service
class AritegService(
    private val metaService: AritegMetaService,
    private val storageService: AritegStorageService,
    private val lockService: DistributedLockService
) {
    private val logger = LoggerFactory.getLogger(AritegService::class.java)

    /**
     * Return true if primary collided, return false if not.
     * Return null if not found
     * */
    fun checkCollision(primary: Multihash, secondary: Multihash): Boolean? {
        // find meta, return null if not found
        val meta = metaService.findMeta(primary) ?: return null
        // if secondary not equal, is collided
        return meta.secondaryMultihash != secondary
    }

    fun multihashExists(primary: Multihash): Boolean {
        return metaService.multihashExists(primary)
    }

    private fun preWriteCheck(link: AritegLink) {
        val multihash = link.multihash.toMultihash()
        require(multihashExists(multihash)) { "Sub link not exists: $multihash" }
    }

    /**
     * Write a proto (BLOB, LIST, TREE or COMMIT) into the system.
     * This method will check with the meta db and see if we can do the
     * deduplication. It will handle meta updates etc.
     * */
    fun writeProto(name: String, proto: AritegObject): WriteReceipt {
        when (proto) {
            is BlobObject -> { // No need to check blob, allow duplicate write
            }
            is ListObject -> { // make sure all sub links are exists
                proto.list.forEach {
                    require(it.type == AritegObjectType.BLOB || it.type == AritegObjectType.LIST) {
                        "Unsupported sub link type ${it.type}, allow: BLOB and LIST"
                    }
                    preWriteCheck(it)
                }
            }
            is TreeObject -> { // make sure all sub links are exists
                proto.links.forEach {
                    require(
                        it.type == AritegObjectType.BLOB
                                || it.type == AritegObjectType.LIST
                                || it.type == AritegObjectType.TREE
                    ) {
                        "Unsupported sub link type ${it.type}, allow: BLOB, LIST and TREE"
                    }
                    preWriteCheck(it)
                }
            }
            is CommitObject -> {
                require(proto.authorLink.type != AritegObjectType.COMMIT) { "Author link cannot be COMMIT" }
                preWriteCheck(proto.authorLink)
                require(proto.parentLink.type == AritegObjectType.COMMIT) { "Parent link must be COMMIT" }
                if (!proto.parentLink.multihash.isEmpty) {
                    // check parent link if and only if it's not empty
                    preWriteCheck(proto.parentLink)
                }
                require(proto.committedObjectLink.type != AritegObjectType.COMMIT) { "Content link cannot be COMMIT" }
                preWriteCheck(proto.committedObjectLink)
            }
            else -> throw IllegalStateException("Unchecked operation")
        }
        // Use AtomicReference to ensure the lock is volatile across threads
        val lockRef: AtomicReference<EtcdSimpleLock?> = AtomicReference(null)
        // check passed, do write
        val (link, completionFuture) = storageService.store(name, proto) { primary: Multihash, secondary: Multihash ->
            // lock the primary
            val lock = lockService.getLock(primary)
            lockRef.set(lock) // save the lock
            lockService.lock(lock)
            if (metaService.createNewEntity(primary, secondary, proto.getObjectType())) {
                // get true -> this is a new proto
                logger.debug("Confirm writing {}", primary.toBase58())
                // newly created, keep locking and confirm writing
                return@store true
            } else {
                // get false -> the primary hash exists
                // check storage, make sure the proto is there
                if (storageService.queryStatus(newLink(primary, proto.getObjectType())) == null) {
                    logger.warn("Proto {} is missing!", primary.toBase58())
                    // keep locking and confirm writing
                    return@store true
                } else {
                    logger.debug("Skip writing {}", primary.toBase58())
                    // release lock and cancel current writing
                    lockService.unlock(lock).also { lockRef.set(null) }
                    return@store false
                }
            }
        }
        val future = completionFuture
            .thenAccept { primary: Multihash? ->
                if (primary != null) {
                    logger.debug("Finish writing {}", primary.toBase58())
                    // unlock only when this writing op is done
                    lockRef.get()?.let { lockService.unlock(it) }
                }
            }
        return WriteReceipt(link, future)
    }

    fun updateMediaType(link: AritegLink, mediaType: String?) {
        metaService.updateMediaType(link.multihash.toMultihash(), mediaType)
    }

    fun readBlob(link: AritegLink): BlobObject = storageService.loadProto(link) as BlobObject

    fun readList(link: AritegLink): ListObject = storageService.loadProto(link) as ListObject

    fun readTree(link: AritegLink): TreeObject = storageService.loadProto(link) as TreeObject

    fun readCommit(link: AritegLink): CommitObject = storageService.loadProto(link) as CommitObject

    fun renameLink(link: AritegLink, newName: String): AritegLink = link.toBuilder().setName(newName).build()

    fun deleteLink(link: AritegLink) {
        metaService.deleteByPrimaryHash(link.multihash.toMultihash())
        storageService.deleteProto(link)
    }

    /**
     * Resolve all related links.
     *
     * @param fullCommit true if you want to resolve the history commits.
     */
    @JvmOverloads
    fun resolveLinks(link: AritegLink, fullCommit: Boolean = false): List<AritegLink> {
        val result: MutableList<AritegLink> = LinkedList()
        // add the link itself
        result.add(link)
        when (link.type) {
            AritegObjectType.LIST -> {
                // add all sub links
                val (list) = readList(link) // pattern match
                for (aritegLink in list) {
                    if (aritegLink.type == AritegObjectType.BLOB) {
                        // add blob without resolve again
                        result.add(aritegLink)
                    } else {
                        // resolve
                        result.addAll(resolveLinks(aritegLink, fullCommit))
                    }
                }
            }
            AritegObjectType.TREE -> {
                // add all sub links
                val (links) = readTree(link) // pattern match
                for (aritegLink in links) {
                    if (aritegLink.type == AritegObjectType.BLOB) {
                        // add blob without resolve again
                        result.add(aritegLink)
                    } else {
                        // resolve
                        result.addAll(resolveLinks(aritegLink, fullCommit))
                    }
                }
            }
            AritegObjectType.COMMIT -> {
                // add parent, author, and commit target
                // we use pattern match here, since the layout of the proto is unlikely to change
                val (_, _, parentLink, committedObjectLink, authorLink) = readCommit(link)
                result.addAll(resolveLinks(authorLink, fullCommit))
                // Restore history version on condition
                if (fullCommit && !parentLink.multihash.isEmpty) {
                    // not the initial commit, then resolve it
                    result.addAll(resolveLinks(parentLink, true))
                }
                result.addAll(resolveLinks(committedObjectLink, fullCommit))
            }
            else -> {} // nothing to do
        }
        return result
    }

    /**
     * Give a root link and resolve all related link using resolveLinks(link, false).
     */
    fun restore(link: AritegLink, option: RestoreOption?): RestoreReceipt {
        val links = resolveLinks(link)
        links.forEach { storageService.restoreLink(it, option) }
        return RestoreReceipt(links)
    }

    /**
     * Probe the status of a give primary hash.
     *
     * Return null if not found.
     */
    fun probe(primary: Multihash): ProbeReceipt? {
        // find type first
        val (secondary, objectType, mediaType) = metaService.findMeta(primary) ?: return null
        val link = newLink(primary, objectType)
        // then check the storage status
        val status = storageService.queryStatus(link)
        if (status == null) {
            // Illegal status but should not throw error
            // log it and count as missing
            logger.error("Link {} found in meta but not in storage", primary.toBase58())
            return null
        }
        return ProbeReceipt(link, secondary, mediaType, status)
    }
}
