package info.skyblond.archivedag.service.impl;

import info.skyblond.archivedag.model.EntityNotFoundException;
import info.skyblond.archivedag.model.bo.FindTypeReceipt;
import info.skyblond.archivedag.model.entity.ProtoMetaEntity;
import info.skyblond.archivedag.repo.ProtoMetaRepository;
import info.skyblond.ariteg.protos.AritegObjectType;
import io.ipfs.multihash.Multihash;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;

@Service
public class AritegMetaService {
    private final ProtoMetaRepository metaRepository;
    private final RedisLockService lockService;

    public AritegMetaService(ProtoMetaRepository metaRepository, RedisLockService lockService) {
        this.metaRepository = metaRepository;
        this.lockService = lockService;
    }

    /**
     * Query the object type and mediaType.
     * Return null if not find.
     */
    public FindTypeReceipt findType(Multihash primary) {
        ProtoMetaEntity meta = this.metaRepository.findByPrimaryHash(primary.toBase58());
        if (meta == null) {
            return null;
        } else {
            return new FindTypeReceipt(meta.getObjectType(), meta.getMediaType());
        }
    }

    /**
     * Update mediaType
     */
    @Transactional
    public boolean updateMediaType(Multihash primary, String mediaType) {
        String p = primary.toBase58();
        if (!this.metaRepository.existsByPrimaryHash(p)) {
            // not found
            return false;
        }
        this.metaRepository.updateMediaType(p, mediaType);
        return true;
    }

    /**
     * Try lock the primary hash with the secondary hash
     */
    public void lock(Multihash primary) {
        this.lockService.lock(primary.toBase58());
    }

    public void unlock(Multihash primary) {
        this.lockService.unlock(primary.toBase58());
    }

    /**
     * Save new entry if the primary hash is not exists.
     * Might not be atomic, so lock before write.
     * Return false if the primary hash has already existed.
     */
    @Transactional
    public boolean createNewEntity(Multihash primaryHash, Multihash secondaryHash, AritegObjectType objectType, String mediaType) {
        String primaryBase58 = primaryHash.toBase58();
        String secondaryBase58 = secondaryHash.toBase58();
        if (this.metaRepository.existsByPrimaryHash(primaryBase58)) {
            // primary hash exist, check the secondary
            if (this.metaRepository.existsByPrimaryHashAndSecondaryHash(primaryBase58, secondaryBase58)) {
                // if the secondary hash is same too, then return false
                return false;
            } else {
                // if secondary hash not exists, then report hash collision
                throw new IllegalStateException("Hash collision detected: " + primaryBase58);
            }
        }
        ProtoMetaEntity entity = new ProtoMetaEntity(primaryBase58, secondaryBase58, objectType, mediaType);
        this.metaRepository.save(entity);
        return true;
    }

    public boolean createNewEntity(Multihash primaryHash, Multihash secondaryHash, AritegObjectType objectType) {
        return createNewEntity(primaryHash, secondaryHash, objectType, null);
    }

    /**
     * Delete proto's meta and return the deleted one. Return null if not found.
     */
    @Transactional
    public void deleteByPrimaryHash(Multihash primary) {
        if (!this.metaRepository.existsByPrimaryHash(primary.toBase58())) {
            throw new EntityNotFoundException("Proto " + primary.toBase58());
        }
        this.metaRepository.deleteByPrimaryHash(primary.toBase58());
    }
}
