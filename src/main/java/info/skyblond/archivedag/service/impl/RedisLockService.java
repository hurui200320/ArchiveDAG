package info.skyblond.archivedag.service.impl;

import info.skyblond.archivedag.service.intf.DistributedLockService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.redis.util.RedisLockRegistry;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

@Service
public class RedisLockService implements DistributedLockService {
    private final Logger logger = LoggerFactory.getLogger(RedisLockService.class);

    private static final long DEFAULT_EXPIRE_UNUSED = 60000L;

    private final RedisLockRegistry redisLockRegistry;

    public RedisLockService(RedisLockRegistry redisLockRegistry) {
        this.redisLockRegistry = redisLockRegistry;
    }

    @Override
    public void lock(String lockKey) {
        Lock lock = this.obtainLock(lockKey);
        lock.lock();
    }

    @Override
    public boolean tryLock(String lockKey) {
        Lock lock = this.obtainLock(lockKey);
        return lock.tryLock();
    }

    @Override
    public boolean tryLock(String lockKey, long duration, TimeUnit unit) {
        Lock lock = this.obtainLock(lockKey);
        try {
            return lock.tryLock(duration, unit);
        } catch (InterruptedException e) {
            return false;
        }
    }

    @Override
    public void unlock(String lockKey) {
        try {
            Lock lock = this.obtainLock(lockKey);
            lock.unlock();
            this.redisLockRegistry.expireUnusedOlderThan(RedisLockService.DEFAULT_EXPIRE_UNUSED);
        } catch (Exception e) {
            this.logger.error("Cannot unlock {}", lockKey, e);
        }
    }

    private Lock obtainLock(String lockKey) {
        return this.redisLockRegistry.obtain(lockKey);
    }
}
