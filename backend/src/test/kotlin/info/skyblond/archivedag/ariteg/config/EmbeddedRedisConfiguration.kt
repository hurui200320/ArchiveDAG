package info.skyblond.archivedag.ariteg.config

import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.data.redis.RedisProperties
import org.springframework.boot.test.context.TestConfiguration
import redis.embedded.RedisServer
import javax.annotation.PostConstruct
import javax.annotation.PreDestroy

@TestConfiguration
class EmbeddedRedisConfiguration(
    redisProperties: RedisProperties
) {
    private val logger = LoggerFactory.getLogger(EmbeddedRedisConfiguration::class.java)
    private val redisServer: RedisServer

    init {
        redisServer = RedisServer(redisProperties.port)
    }

    @PostConstruct
    fun postConstruct() {
        logger.info("Start embedded redis server on port ${redisServer.ports()}")
        try {
            redisServer.start()
        } catch (e: RuntimeException) {
            logger.warn("Cannot create embedded redis, might already started", e)
        }
    }

    @PreDestroy
    fun preDestroy() {
        logger.info("Embedded redis server stopped")
        redisServer.stop()
    }
}
