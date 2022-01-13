package info.skyblond.archivedag.commons.service

import io.etcd.jetcd.ByteSequence
import io.etcd.jetcd.Client
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.Lock


/**
 * This lock is not thread safe. Do not share it with other threads.
 * */

class EtcdSimpleLock(
    etcdClient: Client,
    val lockKey: String,
    private val ttlInSeconds: Long,
    private val timeoutInMs: Long,
    private val service: ScheduledExecutorService
) : Lock {
    private val leaseClient = etcdClient.leaseClient
    private val lockClient = etcdClient.lockClient

    @Volatile
    var lockPath: ByteSequence? = null
        private set

    @Volatile
    private var leaseId = 0L

    @Volatile
    private var future: ScheduledFuture<*>? = null

    override fun lock() {
        if (lockPath != null) {
            // lock twice
            return
        }
        // new lease
        leaseId = leaseClient.grant(ttlInSeconds).get().id
        val period = ttlInSeconds - ttlInSeconds / 5
        future = service.scheduleAtFixedRate(
            { leaseClient.keepAliveOnce(leaseId).get() },
            period, period, TimeUnit.SECONDS
        )
        // try lock...
        while (true) {
            try {
                lockPath = lockClient.lock(
                    ByteSequence.from(lockKey.encodeToByteArray()), leaseId
                ).get(timeoutInMs, TimeUnit.MILLISECONDS).key
                break
            } catch (timeout: TimeoutException) {
                // timeout...
            }
        }
    }

    override fun unlock() {
        if (lockPath == null) {
            // no lock
            return
        }
        // stop the renewal service
        future?.cancel(true)?.also { future = null }
        // remove lease
        leaseClient.revoke(leaseId).get()
        // unlock
        lockClient.unlock(lockPath).get().also { lockPath = null }
    }

    override fun lockInterruptibly() {
        throw UnsupportedOperationException()
    }

    override fun tryLock(): Boolean {
        throw UnsupportedOperationException()
    }

    override fun tryLock(time: Long, unit: TimeUnit): Boolean {
        throw UnsupportedOperationException()
    }

    override fun newCondition(): Condition {
        throw UnsupportedOperationException()
    }
}
