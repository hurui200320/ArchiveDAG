package info.skyblond.archivedag.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.boot.test.context.TestConfiguration;
import redis.embedded.RedisServer;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

@TestConfiguration
public class EmbeddedRedisConfiguration {
    private final Logger logger = LoggerFactory.getLogger(EmbeddedRedisConfiguration.class);
    private final RedisServer redisServer;

    public EmbeddedRedisConfiguration(RedisProperties redisProperties) {
        this.redisServer = new RedisServer(redisProperties.getPort());
    }

    @PostConstruct
    public void postConstruct() {
        this.logger.info("Start embedded redis server on port {}",
                this.redisServer.ports());
        this.redisServer.start();
    }

    @PreDestroy
    public void preDestroy() {
        this.logger.info("Embedded redis server stopped");
        this.redisServer.stop();
    }
}
