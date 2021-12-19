package info.skyblond.archivedag.ariteg

import info.skyblond.archivedag.ariteg.model.*
import info.skyblond.archivedag.ariteg.model.AritegObjects.extractMultihashFromLink
import info.skyblond.archivedag.ariteg.model.AritegObjects.newLink
import info.skyblond.archivedag.ariteg.protos.AritegLink
import info.skyblond.archivedag.ariteg.protos.AritegObjectType
import info.skyblond.archivedag.ariteg.service.AritegMetaService
import info.skyblond.archivedag.ariteg.storage.AritegStorageService
import info.skyblond.archivedag.ariteg.utils.nop
import io.ipfs.multihash.Multihash
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.util.*

@Service
class AritegService(
    private val metaService: AritegMetaService,
    private val storageService: AritegStorageService
) {
    private val logger = LoggerFactory.getLogger(AritegService::class.java)

    /**
     * Write a proto (BLOB, LIST, TREE or COMMIT) into the system.
     * This method will check with the meta db and see if we can do the
     * deduplication. It will handle meta updates etc.
     * */
    fun writeProto(name: String, proto: AritegObject): WriteReceipt {
        val (link, completionFuture) = storageService.store(name, proto) { primary: Multihash, secondary: Multihash ->
            // lock the primary
            metaService.lock(primary)
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
                    metaService.unlock(primary)
                    return@store false
                }
            }
        }
        val future = completionFuture
            .thenAccept { primary: Multihash? ->
                if (primary != null) {
                    logger.debug("Finish writing {}", primary.toBase58())
                    // unlock only when this writing op is done
                    metaService.unlock(primary)
                }
            }
        return WriteReceipt(link, future)
    }

    /**
     * Read a chunk of data from a blob of list.
     *
     * The link can be: BLOB or LIST
     *
     * Return the mediaType and the input stream representing the data.
     */
    fun readChunk(link: AritegLink): ReadReceipt {
        if (link.type != AritegObjectType.BLOB
            && link.type != AritegObjectType.LIST
        ) {
            throw IllegalArgumentException(
                "Unsupported object type ${link.type.name}, only BLOB or LIST is supported."
            )
        }
        val primary = extractMultihashFromLink(link)
        // by default (null) is `"application/octet-stream`
        // it's ok for missing meta data
        val mediaType = metaService.findType(primary)?.mediaType ?: MediaType.APPLICATION_OCTET_STREAM_VALUE
        // since here we need the proto exists, if not, exception will be thrown.
        // It's caller's responsibility to check before read
        val obj = storageService.loadProto(link)

        // if the link is Blob, then return the content
        if (obj is BlobObject) {
            return ReadReceipt(
                mediaType, ByteArrayInputStream(obj.data.toByteArray())
            )
        }

        // Otherwise, it's List
        val resultStream: InputStream = object : InputStream() {
            // init with all links
            val linkList: MutableList<AritegLink> = (obj as ListObject).list.toMutableList()
            var currentBlob: BlobObject? = null
            var pointerInBlob = 0

            @Synchronized
            private fun fetchNextBlob() {
                // make sure we have more links to read
                while (linkList.isNotEmpty() && currentBlob == null) {
                    // fetch the first link and read it
                    val tempLink = linkList.removeAt(0)
                    val tempObj = storageService.loadProto(tempLink)
                    if (tempObj is BlobObject) {
                        // is blob, read and use it
                        currentBlob = tempObj
                        pointerInBlob = 0
                        break // break the loop, we are done
                    } else {
                        // else, assume is list, add them to the list and try again
                        linkList.addAll(0, (tempObj as ListObject).list)
                    }
                }
            }

            override fun read(): Int {
                if (currentBlob == null) {
                    do { // refresh blob
                        fetchNextBlob()
                    } while (currentBlob != null && currentBlob!!.data.size() == 0)
                    // if we get empty blob, skip it and fetch next
                    // stop when getting null blob
                }
                if (currentBlob == null) {
                    // really the end
                    return -1
                }
                // we got the blob, read the value
                val b: Int = currentBlob!!.data.byteAt(pointerInBlob++).toInt()
                if (pointerInBlob >= currentBlob!!.data.size()) {
                    // if we are the end of blob, release it
                    currentBlob = null
                }
                return b and 0xFF // only keep the low 8 bits for byte
            }
        }
        return ReadReceipt(mediaType, resultStream)
    }

    fun readBlob(link: AritegLink): BlobObject = storageService.loadProto(link) as BlobObject

    fun readList(link: AritegLink): ListObject = storageService.loadProto(link) as ListObject


    fun readTree(link: AritegLink): TreeObject = storageService.loadProto(link) as TreeObject

    fun readCommit(link: AritegLink): CommitObject = storageService.loadProto(link) as CommitObject

    fun renameLink(link: AritegLink, newName: String): AritegLink = link.toBuilder().setName(newName).build()

    fun deleteLink(link: AritegLink) {
        metaService.deleteByPrimaryHash(extractMultihashFromLink(link))
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
            else -> nop() // nothing to do
        }
        return result
    }

    /**
     * Give a root link and resolve all related link using resolveLinks(link, false).
     */
    fun restore(link: AritegLink, option: RestoreOption?): RestoreReceipt {
        val links = resolveLinks(link)
        val future = storageService.restoreLinks(links, option)
        return RestoreReceipt(links, future)
    }

    /**
     * Probe the status of a give primary hash.
     *
     * Return null if not found.
     */
    fun probe(primary: Multihash): ProbeReceipt? {
        // find type first
        val (objectType, mediaType) = metaService.findType(primary) ?: return null
        val link = newLink(primary, objectType)
        // then check the storage status
        val status = storageService.queryStatus(link)
        if (status == null) {
            // Illegal status but should not throw error
            // log it and count as missing
            logger.error("Link {} found in meta but not in storage", primary.toBase58())
            return null
        }
        return ProbeReceipt(link, mediaType, status)
    }
}
