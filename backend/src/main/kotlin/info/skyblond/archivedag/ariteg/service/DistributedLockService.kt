package info.skyblond.archivedag.ariteg.service

import info.skyblond.archivedag.commons.service.EtcdLock
import info.skyblond.archivedag.commons.service.EtcdSimpleLockService
import io.ipfs.multihash.Multihash
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap

@Service
class DistributedLockService(
    private val lockService: EtcdSimpleLockService
) {
    private val logger = LoggerFactory.getLogger(DistributedLockService::class.java)

    private val lockPath = "/application/ariteg/lock/proto"
    private val lockMap = ConcurrentHashMap<Multihash, EtcdLock>()

    /**
     * Try lock the primary hash with the secondary hash
     */
    fun lock(primary: Multihash) {
        val lock = lockService.getLock(lockPath, primary.toBase58())
        lock.lock()
        // save the lock
        lockMap[primary] = lock
    }

    fun unlock(primary: Multihash) {
        val lock = lockMap[primary]
        if (lock == null) {
            logger.warn("Lock for proto ${primary.toBase58()} not found")
            return
        }
        try {
            lock.unlock()
        } catch (e: Exception) {
            logger.error("Cannot unlock proto {}", primary.toBase58(), e)
        }
    }
}
