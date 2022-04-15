package info.skyblond.archivedag.arudaz.config

import info.skyblond.archivedag.commons.service.EtcdConfigService
import io.grpc.ServerBuilder
import net.devh.boot.grpc.server.autoconfigure.GrpcServerFactoryAutoConfiguration
import net.devh.boot.grpc.server.config.GrpcServerProperties
import net.devh.boot.grpc.server.serverfactory.GrpcServerConfigurer
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.AutoConfigureBefore
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ByteArrayResource
import javax.annotation.PostConstruct


/**
 * This config replaces the trusted collection certs for gRPC.
 * By default, gRPC server read CA from yml and files.
 * Here we replace it with the one read from etcd.
 * */
@Configuration
@AutoConfigureBefore(GrpcServerFactoryAutoConfiguration::class)
class GrpcCertConfig(
    val properties: GrpcServerProperties,
    val configService: EtcdConfigService
) {
    private val logger = LoggerFactory.getLogger(GrpcCertConfig::class.java)

    @Bean
    fun GrpcServerConfigurer(): GrpcServerConfigurer {
        return GrpcServerConfigurer { serverBuilder: ServerBuilder<*> ->
            serverBuilder.maxInboundMessageSize(8 * 1024 * 1024)
        }
    }

    @PostConstruct
    fun postConstruct() {
        val securitySetting = properties.security
        if (securitySetting.isEnabled) {
            val content = configService.requireString("arstue/user_cert", "ca_cert_pem")
            logger.info("Grpc server trust cert collection: \n$content")
            securitySetting.trustCertCollection = ByteArrayResource(content.encodeToByteArray())
        }
    }
}
