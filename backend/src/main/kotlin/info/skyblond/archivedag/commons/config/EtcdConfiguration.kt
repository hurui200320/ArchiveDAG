package info.skyblond.archivedag.commons.config

import io.etcd.jetcd.ByteSequence
import io.etcd.jetcd.Client
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration


@Configuration
@EnableConfigurationProperties(EtcdProperties::class)
class EtcdConfiguration(
    private val properties: EtcdProperties,
) {
    private val logger = LoggerFactory.getLogger(EtcdConfiguration::class.java)

    @Bean
    fun etcdClient(): Client {
        logger.info("Using etcd endpoints: ${properties.serverAddr}")
        return Client.builder()
            .endpoints(*properties.serverAddr.toTypedArray())
            .apply {
                if (properties.username.isNotBlank()) {
                    logger.info("Etcd user: ${properties.username}")
                    this.user(ByteSequence.from(properties.username.encodeToByteArray()))
                }
                if (properties.password.isNotBlank()) {
                    this.password(ByteSequence.from(properties.password.encodeToByteArray()))
                }
            }
            .build()
    }
}
