package info.skyblond.ariteg.proto

//import com.google.protobuf.ByteString
//import info.skyblond.archivedag.service.intf.ProtoMetaService
//import info.skyblond.archivedag.service.intf.ProtoStorageService
//import info.skyblond.ariteg.AritegLink
//import info.skyblond.ariteg.AritegObject
//import info.skyblond.ariteg.ObjectType
//import info.skyblond.ariteg.ariteg.multihash.MultihashProviders.notMatch
//import info.skyblond.ariteg.ariteg.objects.CommitObject
//import info.skyblond.ariteg.ariteg.objects.TreeObject
//import info.skyblond.ariteg.ariteg.proto.meta.ProtoMetaService
//import info.skyblond.ariteg.ariteg.proto.storage.MultihashNotMatchException
//import java.io.InputStream
//import java.util.concurrent.CompletableFuture
//import java.util.concurrent.TimeUnit
//import kotlin.random.Random
//
///**
// * The proto write service make use of storage service and meta service to handle
// * IO writes.
// * */
//abstract class ProtoWriteService(
//    protected val metaService: ProtoMetaService,
//    protected val storageService: ProtoStorageService,
//    protected val timeoutMsProvider: () -> Long,
//) {
//    /**
//     * Write a proto into system and properly set the metadata
//     * */
//    fun writeProto(name: String, proto: AritegObject): Pair<AritegLink, CompletableFuture<Void>> =
//        storageService.storeProto(name, proto) { primaryHash, secondaryHash, protoContent ->
//            val token = Random.nextLong()
//            // save the entry
//            var entry = metaService.saveIfPrimaryMultihashNotExists(
//                primaryHash, secondaryHash, proto.type, token
//            )
//            // check hash collision
//            if (entry.secondaryMultihash notMatch protoContent) {
//                // report collision
//                throw MultihashNotMatchException(secondaryHash, entry.secondaryMultihash)
//            }
//
//            // check the token
//            if (entry.temp == null) {
//                // the data is persistent stored
//                return@storeProto false // cancel the writing request
//            }
//            // keep checking
//            while (true) {
//                if (entry.temp == token) {
//                    // we got the token, confirm the writing request
//                    return@storeProto true
//                } else {
//                    // someone is writing, then we wait
//                    val deadline = System.currentTimeMillis() + timeoutMsProvider()
//                    while (System.currentTimeMillis() < deadline)
//                        TimeUnit.MILLISECONDS.sleep(deadline - System.currentTimeMillis())
//                    // and check the result
//                    entry = metaService.getByPrimaryMultihash(primaryHash)
//                        ?: throw AssertionError("Entry is deleted during writing: ${primaryHash.toBase58()}")
//                    // if we get null, then onError is called to handle error
//                }
//
//                if (entry.temp == null) {
//                    // write is done, break loop to cancel this writing
//                    break
//                } else {
//                    // timeout, try to lock the key
//                    metaService.compareAndSetTempFlag(primaryHash, entry.temp!!, token)
//                    // then check in next loop
//                }
//            }
//            false
//        }.let { result ->
//            result.first to result.second.thenAccept { primaryHash ->
//                if (primaryHash != null) {
//                    // writing is done, remove temp.
//                    // get the old value first
//                    metaService.compareAndSetTempFlag(primaryHash, 0L, null)?.let {
//                        metaService.compareAndSetTempFlag(primaryHash, it, null)
//                    }
//                    // it's ok to fail.
//                    // Failed means someone locked the obj and will finish this obj.
//                }
//            }
//        }
//
//    /**
//     * Write a chunk of data into system, return a link
//     * to a blob or list object representing that data.
//     * */
//    fun writeChunk(
//        name: String, inputStream: InputStream,
//        blobSize: Int, listLength: Int
//    ): Pair<AritegLink, CompletableFuture<Void>> {
//        val futureList = mutableListOf<CompletableFuture<Void>>()
//        val linkList = mutableListOf<AritegLink>()
//        var actualCount: Int
//        // This will at least store 1 blob.
//        // if the input stream is empty, then an empty blob is stored.
//        do {
//            val bytes = ByteArray(blobSize)
//            actualCount = inputStream.read(bytes)
//            if (actualCount == -1 && linkList.size == 0) {
//                // write empty block only when link list is empty
//                actualCount = 0
//            }
//            // skip the ending where count = -1
//            if (actualCount >= 0) {
//                val blobObject = AritegObject.newBuilder()
//                    .setType(ObjectType.BLOB)
//                    .setData(ByteString.copyFrom(bytes, 0, actualCount))
//                    .build()
//                writeProto("", blobObject).let {
//                    linkList.add(it.first)
//                    futureList.add(it.second)
//                }
//            }
//        } while (actualCount > 0)
//
//        var i = 0
//        while (linkList.size > 1) {
//            // merging blob list to list
//            val list = List(minOf(listLength, linkList.size - i)) { linkList.removeAt(i) }
//            val listObject = AritegObject.newBuilder()
//                .setType(ObjectType.LIST)
//                .addAllLinks(list)
//                .build()
//            writeProto("", listObject).let {
//                linkList.add(i++, it.first)
//                futureList.add(it.second)
//            }
//            // reset i if remains link are not enough for a new list
//            if (i + listLength > linkList.size) i = 0
//        }
//
//        val returnedFuture = CompletableFuture.allOf(*futureList.toTypedArray())
//        // The result is combine all link into one single root
//        return linkList[0].toBuilder().setName(name).build() to returnedFuture
//    }
//
//    /**
//     * Pack a list of link into a tree object.
//     * */
//    fun packLinks(name: String, links: List<AritegLink>): Pair<AritegLink, CompletableFuture<Void>> =
//        writeProto(name, TreeObject(links).toProto())
//
//    /**
//     * Make a commit object with a given link
//     * */
//    fun commitLink(
//        name: String, unixTimestamp: Long,
//        message: String, parentLink: AritegLink,
//        committedObjectLink: AritegLink, authorLink: AritegLink
//    ): Pair<AritegLink, CompletableFuture<Void>> =
//        writeProto(
//            name, CommitObject(
//                unixTimestamp, message, parentLink, committedObjectLink, authorLink
//            ).toProto()
//        )
//}
