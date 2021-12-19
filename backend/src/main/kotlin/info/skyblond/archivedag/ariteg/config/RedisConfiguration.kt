package info.skyblond.archivedag.ariteg.config

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.PropertyAccessor
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.core.*
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializer
import org.springframework.data.redis.serializer.StringRedisSerializer
import org.springframework.integration.redis.util.RedisLockRegistry

/**
 * https://github.com/javastacks/spring-boot-best-practice/blob/1729f4c55942f4dc8ef80dace9585c5bd5deddfd/spring-boot-redis/src/main/java/cn/javastack/springboot/redis/config/RedisConfig.java
 */
@Configuration
class RedisConfiguration {
    @Bean
    fun redisTemplate(factory: RedisConnectionFactory): RedisTemplate<String, Any?> {
        val template = RedisTemplate<String, Any?>()
        template.setConnectionFactory(factory)
        val stringSerializer = StringRedisSerializer()
        val jacksonSerializer = getJacksonSerializer()
        template.keySerializer = stringSerializer
        template.valueSerializer = jacksonSerializer
        template.hashKeySerializer = stringSerializer
        template.hashValueSerializer = jacksonSerializer
        template.setEnableTransactionSupport(true)
        template.afterPropertiesSet()
        return template
    }

    private fun getJacksonSerializer(): RedisSerializer<Any?> {
        val om = ObjectMapper()
        om.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY)
        om.activateDefaultTyping(
            LaissezFaireSubTypeValidator.instance,
            ObjectMapper.DefaultTyping.NON_FINAL
        )
        return GenericJackson2JsonRedisSerializer(om)
    }

    @Bean
    fun hashOperations(redisTemplate: RedisTemplate<String, Any?>): HashOperations<String, String, Any?> {
        return redisTemplate.opsForHash()
    }

    @Bean
    fun valueOperations(redisTemplate: RedisTemplate<String, Any?>): ValueOperations<String, Any?> {
        return redisTemplate.opsForValue()
    }

    @Bean
    fun listOperations(redisTemplate: RedisTemplate<String, Any?>): ListOperations<String, Any?> {
        return redisTemplate.opsForList()
    }

    @Bean
    fun setOperations(redisTemplate: RedisTemplate<String, Any?>): SetOperations<String, Any?> {
        return redisTemplate.opsForSet()
    }

    @Bean
    fun zSetOperations(redisTemplate: RedisTemplate<String, Any?>): ZSetOperations<String, Any?> {
        return redisTemplate.opsForZSet()
    }

    @Bean(destroyMethod = "destroy")
    fun redisLockRegistry(
        redisConnectionFactory: RedisConnectionFactory,
        aritegProperties: AritegProperties
    ): RedisLockRegistry {
        return RedisLockRegistry(
            redisConnectionFactory, "lock",
            aritegProperties.meta.lockExpireTimeUnit.toMillis(
                aritegProperties.meta.lockExpireDuration
            )
        )
    }
}
