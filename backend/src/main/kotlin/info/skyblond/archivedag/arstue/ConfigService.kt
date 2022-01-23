package info.skyblond.archivedag.arstue

import info.skyblond.archivedag.commons.service.EtcdSimpleConfigService
import io.etcd.jetcd.ByteSequence
import io.etcd.jetcd.options.GetOption
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import java.util.*

@Service
class ConfigService(
    private val etcdConfig: EtcdSimpleConfigService,
) {
    private val etcdConfigPrefix = "/application/arstue/config/archive_dag/"

    fun listConfig(keyPrefix: String, pageable: Pageable): Map<String, String?> {
        require(pageable.offset <= Int.MAX_VALUE) { "Offset overflow" }

        val result: MutableMap<String, String?> = HashMap()
        etcdConfig.getConfig(
            ByteSequence.from(etcdConfig.getStringKey(etcdConfigPrefix, keyPrefix).lowercase(), Charsets.UTF_8),
            GetOption.newBuilder().isPrefix(true).withKeysOnly(true).build()
        ).kvs
            .map { it.key.toString().removePrefix(etcdConfigPrefix) }
            .sorted()
            .drop(pageable.offset.toInt())
            .take(pageable.pageSize)
            .forEach {
                val str = etcdConfig.getConfig(etcdConfigPrefix, it)
                result[it] = if (str.isNullOrBlank()) null else str
            }
        return Collections.unmodifiableMap(result)
    }

    fun updateConfig(key: String, value: String?) {
        etcdConfig.putConfig(etcdConfigPrefix, key, value ?: "")
    }

    fun allowGrpcWrite(): Boolean {
        return etcdConfig.getConfig(etcdConfigPrefix, ALLOW_GRPC_WRITE_KEY)?.toBooleanStrictOrNull() ?: false
    }

    companion object {
        const val ALLOW_GRPC_WRITE_KEY = "grpc.allow-write"
    }
}
