package info.skyblond.archivedag.service.impl;

import info.skyblond.archivedag.model.bo.FindTypeReceipt;
import info.skyblond.archivedag.model.entity.MediaTypeEntity;
import info.skyblond.archivedag.model.entity.MetaEntity;
import info.skyblond.archivedag.repo.MediaTypeRepository;
import info.skyblond.archivedag.repo.MetaRepository;
import info.skyblond.archivedag.service.intf.DistributedLockService;
import info.skyblond.archivedag.service.intf.ProtoMetaService;
import info.skyblond.ariteg.ObjectType;
import io.ipfs.multihash.Multihash;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

@Service
public class ProtoMetaServiceImpl implements ProtoMetaService {
    private final Logger logger = LoggerFactory.getLogger(ProtoMetaServiceImpl.class);
    private final MetaRepository metaRepository;
    private final MediaTypeRepository mediaTypeRepository;
    private final DistributedLockService lockService;

    public ProtoMetaServiceImpl(MetaRepository metaRepository, MediaTypeRepository mediaTypeRepository, DistributedLockService lockService) {
        this.metaRepository = metaRepository;
        this.mediaTypeRepository = mediaTypeRepository;
        this.lockService = lockService;
    }

    @Override
    public Multihash findSecondaryMultihash(Multihash primary) {
        MetaEntity result = this.metaRepository.findById(primary.toBase58()).orElse(null);
        if (result == null) {
            return null;
        } else {
            return Multihash.fromBase58(result.getSecondaryMultihashBase58());
        }
    }

    @Override
    public FindTypeReceipt findType(Multihash primary) {
        MetaEntity metaResult = this.metaRepository.findById(primary.toBase58()).orElse(null);
        MediaTypeEntity mediaTypeResult = this.mediaTypeRepository.findById(primary.toBase58()).orElse(null);
        if (metaResult == null) {
            return null;
        } else {
            String mediaType = null;
            if (mediaTypeResult != null) {
                mediaType = mediaTypeResult.getMediaType();
            }
            return new FindTypeReceipt(ObjectType.valueOf(metaResult.getObjType()), mediaType);
        }
    }

    @Override
    public boolean updateMediaType(Multihash primary, String mediaType) {
        if (this.mediaTypeRepository.findById(primary.toBase58()).isPresent()) {
            return false;
        } else {
            this.mediaTypeRepository.save(MediaTypeEntity.builder()
                    .primaryMultihashBase58(primary.toBase58())
                    .mediaType(mediaType)
                    .build());
            return true;
        }
    }

    @Override
    public void lock(Multihash primary) {
        this.lockService.lock(primary.toBase58());
    }

    @Override
    public boolean tryLock(Multihash primary) {
        return this.lockService.tryLock(primary.toBase58());
    }

    @Override
    public boolean tryLock(Multihash primary, long duration, TimeUnit unit) {
        return this.lockService.tryLock(primary.toBase58(), duration, unit);
    }

    @Override
    public void unlock(Multihash primary) {
        this.lockService.unlock(primary.toBase58());
    }

    @Override
    public MetaEntity writeNewEntity(MetaEntity entity) {
        MetaEntity oldEntity = this.metaRepository.findById(entity.getPrimaryMultihashBase58()).orElse(null);
        if (oldEntity == null) {
            this.metaRepository.save(entity);
        }
        return oldEntity;
    }

    @Override
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public MetaEntity deleteByPrimaryHash(Multihash primary) {
        MetaEntity oldEntity = this.metaRepository.findById(primary.toBase58()).orElse(null);
        if (oldEntity != null) {
            this.metaRepository.delete(oldEntity);
            return oldEntity;
        }
        return null;
    }
}
