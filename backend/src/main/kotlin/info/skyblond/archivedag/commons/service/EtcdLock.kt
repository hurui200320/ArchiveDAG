package info.skyblond.archivedag.commons.service

import io.etcd.jetcd.ByteSequence
import io.etcd.jetcd.Client
import java.time.Duration
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.Lock


/**
 * This lock is not thread safe. Do not share it with other threads.
 * */

class EtcdLock(
    etcdClient: Client,
    private val lockKey: String,
    private val duration: Duration,
) : Lock {
    private val leaseClient = etcdClient.leaseClient
    private val lockClient = etcdClient.lockClient

    @Volatile
    private lateinit var lockPath: ByteSequence

    @Volatile
    private var leaseId = 0L

    override fun lock() {
        // new lease
        leaseId = leaseClient.grant(duration.seconds).get().id
        // lock
        lockPath = lockClient.lock(
            ByteSequence.from(lockKey.encodeToByteArray()), leaseId
        ).get().key
        // renew the lease after get the lock
        leaseClient.keepAliveOnce(leaseId).get()
    }

    override fun unlock() {
        // unlock
        lockClient.unlock(lockPath).get()
        // remove lease
        leaseClient.revoke(leaseId)
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
