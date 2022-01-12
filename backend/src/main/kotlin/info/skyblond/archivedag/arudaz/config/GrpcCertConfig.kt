package info.skyblond.archivedag.arudaz.config

import info.skyblond.archivedag.commons.service.EtcdSimpleConfigService
import net.devh.boot.grpc.server.autoconfigure.GrpcServerFactoryAutoConfiguration
import net.devh.boot.grpc.server.config.GrpcServerProperties
import org.springframework.boot.autoconfigure.AutoConfigureBefore
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ByteArrayResource
import javax.annotation.PostConstruct

/**
 * This config replaces the trusted collection certs read from gRPC
 * */
@Configuration
@AutoConfigureBefore(GrpcServerFactoryAutoConfiguration::class)
class GrpcCertConfig(
    val properties: GrpcServerProperties,
    val etcdConfigService: EtcdSimpleConfigService
) {
    @PostConstruct
    fun postConstruct() {
        val securitySetting = properties.security
        if (securitySetting.isEnabled) {
            securitySetting.trustCertCollection = ByteArrayResource(
                etcdConfigService.requireConfig(
                    "/application/arstue/config/user_cert/",
                    "ca_cert_pem"
                ) { it.bytes }
            )
        }
    }
}
