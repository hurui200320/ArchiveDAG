package info.skyblond.archivedag.commons.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties(prefix = "etcd")
data class EtcdProperties(
    var serverAddr: List<String>,
    var username: String = "",
    var password: String = "",
)
