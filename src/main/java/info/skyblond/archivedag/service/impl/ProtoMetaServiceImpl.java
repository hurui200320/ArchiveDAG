package info.skyblond.archivedag.service.impl;

import info.skyblond.archivedag.model.bo.FindTypeReceipt;
import info.skyblond.archivedag.model.entity.MetaEntity;
import info.skyblond.archivedag.repo.MetaRepository;
import info.skyblond.archivedag.service.RedisLockService;
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
    private final RedisLockService redisLockService;

    public ProtoMetaServiceImpl(MetaRepository metaRepository, RedisLockService redisLockService) {
        this.metaRepository = metaRepository;
        this.redisLockService = redisLockService;
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
        MetaEntity result = this.metaRepository.findById(primary.toBase58()).orElse(null);
        if (result == null) {
            return null;
        } else {
            return new FindTypeReceipt(
                    ObjectType.valueOf(result.getObjType()),
                    result.getMediaType()
            );
        }
    }

    @Override
    public boolean updateMediaType(Multihash primary, String mediaType) {
        MetaEntity result = this.metaRepository.findById(primary.toBase58()).orElse(null);
        if (result == null) {
            return false;
        } else {
            result.setMediaType(mediaType);
            this.metaRepository.save(result);
            return true;
        }
    }

    @Override
    public void lock(Multihash primary) {
        this.redisLockService.lock(primary.toBase58());
    }

    @Override
    public boolean tryLock(Multihash primary) {
        return this.redisLockService.tryLock(primary.toBase58());
    }

    @Override
    public boolean tryLock(Multihash primary, long duration, TimeUnit unit) {
        return this.redisLockService.tryLock(primary.toBase58(), duration, unit);
    }

    @Override
    public void unlock(Multihash primary) {
        this.redisLockService.unlock(primary.toBase58());
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
