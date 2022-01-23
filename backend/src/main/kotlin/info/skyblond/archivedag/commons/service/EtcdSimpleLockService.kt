package info.skyblond.archivedag.commons.service

import io.etcd.jetcd.Client
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledThreadPoolExecutor

@Service
class EtcdSimpleLockService(
    private val etcdClient: Client,
    private val config: EtcdSimpleConfigService
) : AutoCloseable {
    private val etcdConfigPrefix = "/application/common/config/"
    private val lockTtlEtcdConfigKey = "lock_ttl_in_sec"
    private val lockTimeoutEtcdConfigKey = "lock_timeout_in_ms"

    private val logger = LoggerFactory.getLogger(EtcdSimpleLockService::class.java)
    private val defaultTtl: Long = 900
    private val defaultTimeout: Long = 1000
    private val executor: ScheduledThreadPoolExecutor =
        Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors()) as ScheduledThreadPoolExecutor

    init {
        // remove canceled tasks
        executor.removeOnCancelPolicy = true
        logger.info("Etcd simple lock ttl: ${getLong(lockTtlEtcdConfigKey, defaultTtl)}s")
        logger.info("Etcd simple lock timeout: ${getLong(lockTimeoutEtcdConfigKey, defaultTimeout)}ms")
    }

    private fun setLong(key: String, long: Long) {
        config.putConfig(
            etcdConfigPrefix, key, long.toString()
        )
    }

    private fun getLong(key: String, default: Long): Long {
        val text = config.getConfig(etcdConfigPrefix, key)
        if (text == null) {
            logger.warn(
                "Config: ${
                    config.getStringKey(etcdConfigPrefix, key)
                } not found, use default value: $default"
            )
            setLong(key, default)
            return default
        }
        return text.toLong()
    }

    fun getLock(lockPath: String, key: String): EtcdSimpleLock {
        return EtcdSimpleLock(
            etcdClient = etcdClient, lockKey = "$lockPath:$key",
            ttlInSeconds = getLong(lockTtlEtcdConfigKey, defaultTtl),
            timeoutInMs = getLong(lockTimeoutEtcdConfigKey, defaultTimeout),
            service = executor
        )
    }

    override fun close() {
        executor.shutdown()
    }
}
