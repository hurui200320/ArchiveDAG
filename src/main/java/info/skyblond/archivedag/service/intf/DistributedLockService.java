package info.skyblond.archivedag.service.intf;

import java.util.concurrent.TimeUnit;

public interface DistributedLockService {
    /**
     * Lock the key. Await if the key not available.
     * Return after get the key.
     * */
    void lock(String lockKey);

    /**
     * Try lock the key.
     * Return false immediately if the key not available.
     * */
    boolean tryLock(String lockKey);

    /**
     * Try lock the key in the given time duration.
     * Return false if we cannot lock the key in given time.
     * */
    boolean tryLock(String lockKey, long duration, TimeUnit unit);

    /**
     * Unlock the key.
     * */
    void unlock(String lockKey);
}
