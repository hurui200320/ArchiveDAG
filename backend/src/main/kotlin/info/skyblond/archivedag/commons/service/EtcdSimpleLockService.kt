package info.skyblond.archivedag.commons.service

import io.etcd.jetcd.Client
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class EtcdSimpleLockService(
    private val etcdClient: Client,
    private val config: EtcdConfigService
) {
    private val etcdNamespace = "common"
    private val lockTtlEtcdConfigKey = "lock_ttl_in_sec"
    private val lockTimeoutEtcdConfigKey = "lock_timeout_in_ms"

    private val logger = LoggerFactory.getLogger(EtcdSimpleLockService::class.java)
    private val defaultTtl: Long = 300
    private val defaultTimeout: Long = 1000

    init {
        logger.info("Etcd simple lock ttl: ${getLongWithDefault(lockTtlEtcdConfigKey, defaultTtl)}s")
        logger.info("Etcd simple lock timeout: ${getLongWithDefault(lockTimeoutEtcdConfigKey, defaultTimeout)}ms")
    }

    private fun getLongWithDefault(key: String, default: Long): Long {
        return config.getLong(etcdNamespace, key) ?: run {
            logger.warn(
                "Config: ${
                    config.getStringKey(etcdNamespace, key)
                } not found, use default value: $default"
            )
            config.setLong(etcdNamespace, key, default)
            default
        }
    }

    fun getLock(lockPath: String, key: String): EtcdSimpleLock {
        return EtcdSimpleLock(
            etcdClient = etcdClient, lockKey = "$lockPath:$key",
            ttlInSeconds = getLongWithDefault(lockTtlEtcdConfigKey, defaultTtl),
            timeoutInMs = getLongWithDefault(lockTimeoutEtcdConfigKey, defaultTimeout)
        )
    }
}
