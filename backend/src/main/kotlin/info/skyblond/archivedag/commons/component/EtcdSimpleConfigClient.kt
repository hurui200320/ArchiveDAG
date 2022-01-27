package info.skyblond.archivedag.commons.component

import io.etcd.jetcd.ByteSequence
import io.etcd.jetcd.Client
import io.etcd.jetcd.kv.DeleteResponse
import io.etcd.jetcd.kv.GetResponse
import io.etcd.jetcd.kv.PutResponse
import io.etcd.jetcd.options.DeleteOption
import io.etcd.jetcd.options.GetOption
import io.etcd.jetcd.options.PutOption
import org.springframework.stereotype.Component

/**
 * This is a simple config service based on etcd.
 * */
@Component
class EtcdSimpleConfigClient(
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
}
