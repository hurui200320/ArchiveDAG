package info.skyblond.archivedag.ariteg.service

import info.skyblond.archivedag.ariteg.entity.ProtoMetaEntity
import info.skyblond.archivedag.ariteg.model.FindMetaReceipt
import info.skyblond.archivedag.ariteg.protos.AritegObjectType
import info.skyblond.archivedag.ariteg.repo.ProtoMetaRepository
import info.skyblond.archivedag.commons.EntityNotFoundException
import io.ipfs.multihash.Multihash
import org.springframework.stereotype.Service
import javax.transaction.Transactional

@Service
class AritegMetaService(private val metaRepository: ProtoMetaRepository, private val lockService: RedisLockService) {
    /**
     * Query the object type and mediaType.
     * Return null if not find.
     */
    fun findMeta(primary: Multihash): FindMetaReceipt? {
        val meta = metaRepository.findByPrimaryHash(primary.toBase58())
        return if (meta == null) {
            null
        } else {
            FindMetaReceipt(
                Multihash.fromBase58(meta.secondaryHash),
                meta.objectType, meta.mediaType
            )
        }
    }

    /**
     * Update mediaType, the media type can be null
     */
    @Transactional
    fun updateMediaType(primary: Multihash, mediaType: String?): Boolean {
        val p = primary.toBase58()
        if (!metaRepository.existsByPrimaryHash(p)) {
            // not found
            return false
        }
        metaRepository.updateMediaType(p, mediaType)
        return true
    }

    /**
     * Try lock the primary hash with the secondary hash
     */
    fun lock(primary: Multihash) {
        lockService.lock("archivedag.ariteg.proto.lock." + primary.toBase58())
    }

    fun unlock(primary: Multihash) {
        lockService.unlock("archivedag.ariteg.proto.lock." + primary.toBase58())
    }

    fun multihashExists(primary: Multihash): Boolean {
        return metaRepository.existsByPrimaryHash(primary.toBase58())
    }

    /**
     * Save new entry if the primary hash is not exists.
     * Might not be atomic, so lock before write.
     * Return false if the primary hash has already existed.
     */
    @Transactional
    fun createNewEntity(
        primaryHash: Multihash,
        secondaryHash: Multihash,
        objectType: AritegObjectType,
        mediaType: String?
    ): Boolean {
        val primaryBase58 = primaryHash.toBase58()
        val secondaryBase58 = secondaryHash.toBase58()
        if (metaRepository.existsByPrimaryHash(primaryBase58)) {
            // primary hash exist, check the secondary
            return if (metaRepository.existsByPrimaryHashAndSecondaryHash(primaryBase58, secondaryBase58)) {
                // if the secondary hash is same too, then return false
                false
            } else {
                // if secondary hash not exists, then report hash collision
                throw IllegalStateException("Hash collision detected: $primaryBase58")
            }
        }
        val entity = ProtoMetaEntity(primaryBase58, secondaryBase58, objectType, mediaType)
        metaRepository.save(entity)
        return true
    }

    fun createNewEntity(primaryHash: Multihash, secondaryHash: Multihash, objectType: AritegObjectType): Boolean {
        return createNewEntity(primaryHash, secondaryHash, objectType, null)
    }

    /**
     * Delete proto's meta and return the deleted one. Return null if not found.
     */
    @Transactional
    fun deleteByPrimaryHash(primary: Multihash) {
        if (!metaRepository.existsByPrimaryHash(primary.toBase58())) {
            throw EntityNotFoundException("Proto " + primary.toBase58())
        }
        metaRepository.deleteByPrimaryHash(primary.toBase58())
    }
}
