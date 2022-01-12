package info.skyblond.archivedag.commons.service

import io.etcd.jetcd.Client
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Duration

@Service
class EtcdSimpleLockService(
    private val etcdClient: Client,
    private val config: EtcdSimpleConfigService
) {
    private val etcdConfigPrefix = "/application/common/config/"
    private val lockDurationEtcdConfigKey = "lock_duration"

    private val logger = LoggerFactory.getLogger(EtcdSimpleLockService::class.java)

    fun setLockDuration(duration: Duration) {
        config.putConfig(
            etcdConfigPrefix, lockDurationEtcdConfigKey,
            duration.toString()
        )
    }

    fun getLockDuration(): Duration {
        val text = config.getConfig(etcdConfigPrefix, lockDurationEtcdConfigKey)
        if (text == null) {
            logger.warn(
                "Config: ${
                    config.getStringKey(
                        etcdConfigPrefix,
                        lockDurationEtcdConfigKey
                    )
                } not found, use default value"
            )
            return Duration.ofMinutes(15)
        }
        return Duration.parse(text)
    }

    fun getLock(lockPath: String, key: String): EtcdLock {
        return EtcdLock(etcdClient, "$lockPath:$key", getLockDuration())
    }
}
