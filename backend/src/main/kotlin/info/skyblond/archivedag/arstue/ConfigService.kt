package info.skyblond.archivedag.arstue

import info.skyblond.archivedag.arstue.entity.ConfigEntity
import info.skyblond.archivedag.arstue.repo.ConfigRepository
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import java.util.*
import javax.transaction.Transactional

@Service
class ConfigService(
    private val configRepository: ConfigRepository
) {
    fun listConfig(keyPrefix: String, pageable: Pageable): Map<String, String?> {
        val result: MutableMap<String, String?> = HashMap()
        configRepository.findAllByKeyStartingWith(keyPrefix.lowercase(Locale.getDefault()), pageable)
            .forEach { result[it.key] = it.value }
        return Collections.unmodifiableMap(result)
    }

    @Transactional
    fun updateConfig(key: String, value: String?) {
        val entity = ConfigEntity(key.lowercase(Locale.getDefault()), value)
        configRepository.save(entity)
    }

    fun allowGrpcWrite(): Boolean {
        val entity = configRepository.findByKey(ALLOW_GRPC_WRITE_KEY) ?: return false
        return java.lang.Boolean.parseBoolean(entity.value)
    }

    companion object {
        const val ALLOW_GRPC_WRITE_KEY = "archive-dag.grpc.allow-write"
    }
}
