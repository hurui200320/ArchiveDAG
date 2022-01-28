package info.skyblond.archivedag.commons.service

import info.skyblond.archivedag.commons.component.EtcdSimpleConfigClient
import io.etcd.jetcd.ByteSequence
import io.etcd.jetcd.KeyValue
import org.springframework.stereotype.Service

@Service
class EtcdConfigService(
    private val configClient: EtcdSimpleConfigClient
) {
    val applicationPrefix = "/archive_dag/config".removeSuffix("/")

    /**
     * convert prefix and key to a full qualified key
     * */
    fun getStringKey(namespace: String, key: String): String =
        "$applicationPrefix/${namespace.removeSurrounding("/")}/${key.removeSurrounding("/")}"

    /**
     * Map prefix and key to the real etcd key.
     * */
    private fun getKey(namespace: String, key: String): ByteSequence =
        ByteSequence.from(getStringKey(namespace, key).encodeToByteArray())


    /**
     * Set a byte array to a given key, return old key-value pair.
     * */
    fun setByteArray(namespace: String, key: String, value: ByteArray): KeyValue? {
        val resp = configClient.putConfig(getKey(namespace, key), ByteSequence.from(value))
        return if (resp.hasPrevKv()) resp.prevKv else null
    }

    /**
     * Get a byte array to a given key, return null is not found
     * */
    fun getByteArray(namespace: String, key: String): ByteArray? =
        configClient.getConfig(getKey(namespace, key)).kvs
            .let { if (it.isEmpty()) null else it[0] }
            ?.value?.bytes

    /**
     * Get a byte array from a given key, throw exception if not found.
     * */
    fun requireByteArray(namespace: String, key: String): ByteArray =
        requireNotNull(getByteArray(namespace, key)) { "Config: ${getStringKey(namespace, key)} not found" }


    /**
     * Set a string to a given key, return old key-value pair.
     * */
    fun setString(namespace: String, key: String, value: String): KeyValue? {
        val resp = configClient.putConfig(getKey(namespace, key), ByteSequence.from(value, Charsets.UTF_8))
        return if (resp.hasPrevKv()) resp.prevKv else null
    }

    /**
     * Get a string from a given key. Return null if not found.
     * */
    fun getString(namespace: String, key: String): String? =
        configClient.getConfig(getKey(namespace, key)).kvs
            .let { if (it.isEmpty()) null else it[0] }
            ?.value?.toString()

    fun getString(namespace: String, key: String, default: String): String =
        getString(namespace, key) ?: default

    /**
     * Get a string from a given key. Throw exception if not found.
     * */
    fun requireString(namespace: String, key: String): String =
        requireNotNull(getString(namespace, key)) { "Config: ${getStringKey(namespace, key)} not found" }


    fun setLong(namespace: String, key: String, value: Long): KeyValue? =
        setString(namespace, key, value.toString())

    fun getLong(namespace: String, key: String): Long? =
        getString(namespace, key)?.toLongOrNull()

    fun getLong(namespace: String, key: String, default: Long): Long =
        getLong(namespace, key) ?: default

    fun requireLong(namespace: String, key: String): Long =
        requireNotNull(getLong(namespace, key)) { "Config: ${getStringKey(namespace, key)} not found" }


    fun setInt(namespace: String, key: String, value: Int): KeyValue? =
        setString(namespace, key, value.toString())

    fun getInt(namespace: String, key: String): Int? =
        getString(namespace, key)?.toIntOrNull()

    fun getInt(namespace: String, key: String, default: Int): Int =
        getInt(namespace, key) ?: default

    fun requireInt(namespace: String, key: String): Int =
        requireNotNull(getInt(namespace, key)) { "Config: ${getStringKey(namespace, key)} not found" }

}
