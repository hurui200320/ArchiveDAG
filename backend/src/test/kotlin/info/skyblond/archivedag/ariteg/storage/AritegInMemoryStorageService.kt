package info.skyblond.archivedag.ariteg.storage

import com.google.protobuf.ByteString
import info.skyblond.archivedag.ariteg.model.AritegObject
import info.skyblond.archivedag.ariteg.model.RestoreOption
import info.skyblond.archivedag.ariteg.model.StorageStatus
import info.skyblond.archivedag.ariteg.model.StoreReceipt
import info.skyblond.archivedag.ariteg.multihash.MultihashProvider
import info.skyblond.archivedag.ariteg.protos.AritegLink
import info.skyblond.archivedag.ariteg.utils.toMultihash
import info.skyblond.archivedag.ariteg.utils.toMultihashBase58
import io.ipfs.multihash.Multihash
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.function.BiFunction

/**
 * For Test only
 * */
class AritegInMemoryStorageService(
    private val primaryProvider: MultihashProvider,
    private val secondaryProvider: MultihashProvider,
) : AritegStorageService {
    private val contentMap = ConcurrentHashMap<Multihash, AritegObject>()

    override fun store(
        name: String,
        proto: AritegObject,
        checkBeforeWrite: BiFunction<Multihash, Multihash, Boolean>
    ): StoreReceipt {
        val rawBytes = proto.toProto().toByteArray()
        val primaryMultihash = primaryProvider.digest(rawBytes)
        val secondaryMultihash = secondaryProvider.digest(rawBytes)
        // run the check, return if we get false
        val future = CompletableFuture.supplyAsync {
            if (checkBeforeWrite.apply(primaryMultihash, secondaryMultihash)) {
                // check pass, add to map
                contentMap[primaryMultihash] = proto
                primaryMultihash
            } else {
                null
            }
        }

        return StoreReceipt(
            AritegLink.newBuilder()
                .setName(name)
                .setMultihash(ByteString.copyFrom(primaryMultihash.toBytes()))
                .setType(proto.getObjectType())
                .build(), future
        )
    }

    override fun queryStatus(link: AritegLink): StorageStatus? {
        val multihash = link.multihash.toMultihash()
        return if (contentMap.containsKey(multihash)) {
            StorageStatus(
                available = true,
                protoSize = contentMap[multihash]!!.toProto().toByteArray().size.toLong()
            )
        } else {
            null
        }
    }

    override fun restoreLink(link: AritegLink, option: RestoreOption?) {
    }

    override fun loadProto(link: AritegLink): AritegObject {
        val multihash = link.multihash.toMultihash()
        return contentMap[multihash]
            ?: throw IllegalStateException("Cannot load ${link.toMultihashBase58()}: not found")
    }

    override fun deleteProto(link: AritegLink): Boolean {
        val multihash = link.multihash.toMultihash()
        return contentMap.remove(multihash) != null
    }
}
