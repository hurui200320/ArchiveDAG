package info.skyblond.archivedag.ariteg.service

import info.skyblond.archivedag.ariteg.config.AritegProperties
import org.slf4j.LoggerFactory
import org.springframework.integration.redis.util.RedisLockRegistry
import org.springframework.stereotype.Service
import java.util.concurrent.locks.Lock

@Service
class RedisLockService(
    private val aritegProperties: AritegProperties,
    private val redisLockRegistry: RedisLockRegistry
) {
    private val logger = LoggerFactory.getLogger(RedisLockService::class.java)

    /**
     * Lock the key. Await if the key not available.
     * Return after get the key.
     */
    fun lock(lockKey: String) {
        val lock = obtainLock(lockKey)
        lock.lock()
    }

    /**
     * Unlock the key.
     */
    fun unlock(lockKey: String) {
        try {
            val lock = obtainLock(lockKey)
            lock.unlock()
            redisLockRegistry.expireUnusedOlderThan(
                aritegProperties.meta.lockExpireTimeUnit.toMillis(
                    aritegProperties.meta.lockExpireDuration
                )
            )
        } catch (e: Exception) {
            logger.error("Cannot unlock {}", lockKey, e)
        }
    }

    private fun obtainLock(lockKey: String): Lock {
        return redisLockRegistry.obtain(lockKey)
    }
}
