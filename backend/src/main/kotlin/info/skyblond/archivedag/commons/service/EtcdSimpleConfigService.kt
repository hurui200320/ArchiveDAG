package info.skyblond.archivedag.commons.service

import io.etcd.jetcd.ByteSequence
import io.etcd.jetcd.Client
import io.etcd.jetcd.KeyValue
import io.etcd.jetcd.kv.DeleteResponse
import io.etcd.jetcd.kv.GetResponse
import io.etcd.jetcd.kv.PutResponse
import io.etcd.jetcd.options.DeleteOption
import io.etcd.jetcd.options.GetOption
import io.etcd.jetcd.options.PutOption
import org.springframework.stereotype.Service

/**
 * This is a simple config service based on etcd.
 * */
@Service
class EtcdSimpleConfigService(
    private val etcdClient: Client
) {
    /**
     * Raw map of the get method and get the resp immediately.
     * */
    fun getConfig(
        key: ByteSequence, option: GetOption = GetOption.DEFAULT
    ): GetResponse {
        return etcdClient.kvClient.get(key, option).get()
    }

    /**
     * Raw map of the put method and get the resp immediately.
     * */
    fun putConfig(
        key: ByteSequence, value: ByteSequence,
        option: PutOption = PutOption.DEFAULT
    ): PutResponse {
        return etcdClient.kvClient.put(key, value, option).get()
    }

    /**
     * Raw map of the delete method and get the resp immediately.
     * */
    fun deleteConfig(
        key: ByteSequence, option: DeleteOption = DeleteOption.DEFAULT
    ): DeleteResponse {
        return etcdClient.kvClient.delete(key, option).get()
    }

    /**
     * convert prefix and key to a full qualified key
     * */
    fun getStringKey(prefix: String, key: String): String {
        return if (prefix.endsWith("/")) {
            prefix + key
        } else {
            "$prefix/$key"
        }
    }

    /**
     * Map prefix and key to the real etcd key.
     * */
    private fun getKey(prefix: String, key: String): ByteSequence {
        return ByteSequence.from(getStringKey(prefix, key).encodeToByteArray())
    }


    /**
     * Get config with the given prefix and key, map the result to T.
     * */
    fun <T> getConfig(prefix: String, key: String, mapper: (ByteSequence) -> T): T? {
        return getConfig(getKey(prefix, key)).kvs
            .let { if (it.isEmpty()) null else it[0] }
            ?.let { mapper.invoke(it.value) }
    }

    fun <T> requireConfig(prefix: String, key: String, mapper: (ByteSequence) -> T): T {
        return getConfig(prefix, key, mapper) ?: error("Config: ${getStringKey(prefix, key)} not found")
    }

    /**
     * Get a string config
     * */
    fun getConfig(prefix: String, key: String): String? {
        return getConfig(prefix, key) {
            it.toString(Charsets.UTF_8)
        }
    }

    fun requireConfig(prefix: String, key: String): String {
        return getConfig(prefix, key) ?: error("Config: ${getStringKey(prefix, key)} not found")
    }

    /**
     * Put a T as value and map it to bytes.
     * Return old key-value (null if no old value).
     * */
    fun <T> putConfig(prefix: String, key: String, value: T, mapper: (T) -> ByteSequence): KeyValue? {
        return putConfig(getKey(prefix, key), mapper.invoke(value)).prevKv
    }

    /**
     * Put a string as value.
     * Return old key-value (null if no old value).
     * */
    fun putConfig(prefix: String, key: String, value: String): KeyValue? {
        return putConfig(prefix, key, value) { ByteSequence.from(value.encodeToByteArray()) }
    }

    /**
     * Delete config(s).
     * Return deletes key-value list.
     * */
    fun deleteConfig(
        prefix: String, key: String
    ): List<KeyValue> {
        return deleteConfig(getKey(prefix, key)).prevKvs
    }

}
