package info.skyblond.archivedag.arstue

import info.skyblond.archivedag.commons.component.EtcdSimpleConfigClient
import info.skyblond.archivedag.commons.service.EtcdConfigService
import io.etcd.jetcd.ByteSequence
import io.etcd.jetcd.options.GetOption
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import java.util.*

@Service
class ApplicationConfigService(
    private val configClient: EtcdSimpleConfigClient,
    private val configService: EtcdConfigService
) {
    fun listConfig(keyPrefix: String, pageable: Pageable): Map<String, String?> {
        require(pageable.offset <= Int.MAX_VALUE) { "Offset overflow" }

        val result: MutableMap<String, String?> = HashMap()
        configClient.getConfig(
            ByteSequence.from("${configService.applicationPrefix}$keyPrefix".lowercase(), Charsets.UTF_8),
            GetOption.newBuilder().isPrefix(true).withKeysOnly(true).build()
        ).kvs
            .map { it.key.toString() }
            .sorted()
            .drop(pageable.offset.toInt())
            .take(pageable.pageSize)
            .forEach { fullKey ->
                val str = configClient.getConfig(ByteSequence.from(fullKey, Charsets.UTF_8))
                    .let { if (it.kvs.isEmpty()) null else it.kvs[0] }?.value?.toString()
                val key = fullKey.removePrefix(configService.applicationPrefix)
                result[key] = if (str.isNullOrBlank()) null else str
            }
        return Collections.unmodifiableMap(result)
    }

    fun updateConfig(key: String, value: String?) {
        configClient.putConfig(
            ByteSequence.from("${configService.applicationPrefix}$key".lowercase(), Charsets.UTF_8),
            ByteSequence.from(value ?: "", Charsets.UTF_8)
        )
    }

    private fun getConfig(key: String): String? {
        return configClient.getConfig(
            ByteSequence.from("${configService.applicationPrefix}$key".lowercase(), Charsets.UTF_8)
        ).let { if (it.kvs.isEmpty()) null else it.kvs[0] }?.value?.toString()
    }

    fun allowGrpcWrite(): Boolean {
        return getConfig(ALLOW_GRPC_WRITE_KEY)?.toBooleanStrictOrNull() ?: false
    }

    companion object {
        const val ALLOW_GRPC_WRITE_KEY = "/application/grpc/allow-write"
    }
}
