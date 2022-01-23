package info.skyblond.archivedag.ariteg.service

import info.skyblond.archivedag.commons.service.EtcdSimpleLock
import info.skyblond.archivedag.commons.service.EtcdSimpleLockService
import io.ipfs.multihash.Multihash
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class DistributedLockService(
    private val lockService: EtcdSimpleLockService
) {
    private val logger = LoggerFactory.getLogger(DistributedLockService::class.java)

    private val lockPath = "/application/ariteg/lock/proto"

    fun getLock(primary: Multihash): EtcdSimpleLock {
        return lockService.getLock(lockPath, primary.toBase58())
    }

    /**
     * Try lock the primary hash with the secondary hash
     */
    fun lock(lock: EtcdSimpleLock) {
        // The loop will make sure the lock has been acquired
        while (true) {
            try {
                lock.lock()
                break
            } catch (t: Throwable) {
                logger.error("Failed to lock ${lock.lockKey}, retry...", t)
            }
        }
    }

    fun unlock(lock: EtcdSimpleLock) {
        // The `unlock` ensure the renewal is canceled
        // even if the operation is failed, it will expire
        try {
            lock.unlock()
        } catch (e: Exception) {
            logger.error("Cannot unlock ${lock.lockPath}", e)
        }
    }
}
