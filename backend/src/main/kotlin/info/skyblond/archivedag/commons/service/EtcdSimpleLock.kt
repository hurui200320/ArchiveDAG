package info.skyblond.archivedag.commons.service

import io.etcd.jetcd.ByteSequence
import io.etcd.jetcd.Client
import io.etcd.jetcd.lease.LeaseKeepAliveResponse
import io.etcd.jetcd.support.CloseableClient
import io.grpc.stub.StreamObserver
import org.slf4j.LoggerFactory
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
    /**
     * Lifetime of a lease, should bigger than lock timeout
     * */
    private val ttlInSeconds: Long,
    /**
     * Lock timeout, if we cannot get lock in this amount of time,
     * give up and retry.
     * */
    private val timeoutInMs: Long
) : Lock {
    private val logger = LoggerFactory.getLogger(EtcdSimpleLock::class.java)
    private val leaseClient = etcdClient.leaseClient
    private val lockClient = etcdClient.lockClient

    @Volatile
    var lockPath: ByteSequence? = null
        private set

    @Volatile
    private var leaseId = 0L

    @Volatile
    private var renewClient: CloseableClient? = null

    override fun lock() {
        if (lockPath != null) {
            // lock twice
            return
        }
        // try lock...
        while (lockPath == null) {
            // get new lease
            leaseId = leaseClient.grant(ttlInSeconds).get().id
            try {
                lockPath = lockClient.lock(
                    ByteSequence.from(lockKey.encodeToByteArray()), leaseId
                ).get(timeoutInMs, TimeUnit.MILLISECONDS).key
                break // done
            } catch (timeout: TimeoutException) {
                // Timeout, release lease
                leaseClient.revoke(leaseId).get()
            }
            try {
                // wait for sometime to avoid racing
                Thread.sleep(timeoutInMs)
            } catch (_: Throwable) {
                // nothing, it's ok to be interrupted
            }
        }
        // lock done, renew lease regularly
        renewClient = leaseClient.keepAlive(leaseId, object : StreamObserver<LeaseKeepAliveResponse> {
            override fun onNext(value: LeaseKeepAliveResponse?) = Unit

            override fun onError(t: Throwable?) {
                logger.error("Failed to keep lease $leaseId for lock $lockKey", t)
            }

            override fun onCompleted() = Unit
        })
    }

    override fun unlock() {
        if (lockPath == null) {
            // no lock
            return
        }
        // unlock
        lockClient.unlock(lockPath).get().also { lockPath = null }
        // stop the renewal service
        renewClient!!.close().also { renewClient = null }
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
