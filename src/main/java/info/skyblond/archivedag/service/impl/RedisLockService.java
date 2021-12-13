package info.skyblond.archivedag.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.integration.redis.util.RedisLockRegistry;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

@Slf4j
@Service
public class RedisLockService {
    private static final long DEFAULT_EXPIRE_UNUSED = 60000L;

    private final RedisLockRegistry redisLockRegistry;

    public RedisLockService(RedisLockRegistry redisLockRegistry) {
        this.redisLockRegistry = redisLockRegistry;
    }

    /**
     * Lock the key. Await if the key not available.
     * Return after get the key.
     */
    public void lock(String lockKey) {
        Lock lock = this.obtainLock(lockKey);
        lock.lock();
    }

    /**
     * Try lock the key.
     * Return false immediately if the key not available.
     */
    public boolean tryLock(String lockKey) {
        Lock lock = this.obtainLock(lockKey);
        return lock.tryLock();
    }

    /**
     * Try lock the key in the given time duration.
     * Return false if we cannot lock the key in given time.
     */
    public boolean tryLock(String lockKey, long duration, TimeUnit unit) {
        Lock lock = this.obtainLock(lockKey);
        try {
            return lock.tryLock(duration, unit);
        } catch (InterruptedException e) {
            return false;
        }
    }

    /**
     * Unlock the key.
     */
    public void unlock(String lockKey) {
        try {
            Lock lock = this.obtainLock(lockKey);
            lock.unlock();
            this.redisLockRegistry.expireUnusedOlderThan(RedisLockService.DEFAULT_EXPIRE_UNUSED);
        } catch (Exception e) {
            log.error("Cannot unlock {}", lockKey, e);
        }
    }

    private Lock obtainLock(String lockKey) {
        return this.redisLockRegistry.obtain(lockKey);
    }
}
