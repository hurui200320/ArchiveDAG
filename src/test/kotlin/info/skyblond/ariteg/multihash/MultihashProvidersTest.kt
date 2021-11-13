package info.skyblond.ariteg.multihash

import info.skyblond.ariteg.multihash.MultihashProviders.mustMatch
import io.ipfs.multihash.Multihash
import org.junit.jupiter.api.Test

internal class MultihashProvidersTest {

    @Test
    fun testProvides() {
        Multihash.Type.values().forEach { type ->
            try {
                MultihashProviders.fromMultihashType(type)
                    .digest(ByteArray(0)) mustMatch ByteArray(0)
            } catch (_: NotImplementedError) {
            }
        }
    }
}
