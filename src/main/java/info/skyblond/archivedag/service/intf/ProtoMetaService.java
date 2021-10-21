package info.skyblond.archivedag.service.intf;

import info.skyblond.archivedag.model.bo.FindTypeReceipt;
import info.skyblond.archivedag.model.entity.MetaEntity;
import io.ipfs.multihash.Multihash;

import java.util.concurrent.TimeUnit;

public interface ProtoMetaService {

    /**
     * Query the secondary hash
     */
    Multihash findSecondaryMultihash(Multihash primary);

    /**
     * Query the object type and mediaType
     */
    FindTypeReceipt findType(Multihash primary);

    /**
     * Query the object type and mediaType
     */
    boolean updateMediaType(Multihash primary, String mediaType);

    /**
     * Try lock the primary hash with the secondary hash
     */
    void lock(Multihash primary);

    boolean tryLock(Multihash primary);

    boolean tryLock(Multihash primary, long duration, TimeUnit unit);

    void unlock(Multihash primary);

    /**
     * Save new entry if the primary hash is not exists.
     * Might not be atomic, so lock before write.
     * Return already exists one or null.
     */
    MetaEntity writeNewEntity(MetaEntity entity);

    /**
     * Delete proto's meta and return the deleted one. Return null if not found.
     * *Not recommended. Use at your own risk.*
     */
    @Deprecated
    MetaEntity deleteByPrimaryHash(Multihash primary);
}
