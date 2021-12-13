package info.skyblond.archivedag.service.impl;

import info.skyblond.archivedag.config.EmbeddedRedisConfiguration;
import info.skyblond.archivedag.config.WebMvcConfig;
import info.skyblond.ariteg.multihash.MultihashProvider;
import info.skyblond.ariteg.multihash.MultihashProviders;
import io.ipfs.multihash.Multihash;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(
        classes = {EmbeddedRedisConfiguration.class, WebMvcConfig.class}
)
@ActiveProfiles("test")
class AritegMetaServiceTest {

    @Autowired
    AritegMetaService metaService;

    MultihashProvider primary = MultihashProviders.fromMultihashType(Multihash.Type.blake2b_256);


    @Test
    void testTryLock() throws InterruptedException, ExecutionException {
        Multihash key = primary.digest("test_lock_key_no_duration".getBytes(StandardCharsets.UTF_8));
        CompletableFuture.runAsync(() -> assertTrue(this.metaService.tryLock(key))).get();
        assertFalse(this.metaService.tryLock(key));
    }

    @Test
    void testTryLockDuration() throws ExecutionException, InterruptedException {
        Multihash key = primary.digest("test_lock_key_with_duration".getBytes(StandardCharsets.UTF_8));
        CompletableFuture.runAsync(() ->
                assertTrue(this.metaService.tryLock(key, 1, TimeUnit.SECONDS))).get();
        assertFalse(this.metaService.tryLock(key, 1, TimeUnit.SECONDS));
    }

}
