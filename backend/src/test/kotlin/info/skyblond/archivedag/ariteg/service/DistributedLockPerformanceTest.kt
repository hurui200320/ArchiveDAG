package info.skyblond.archivedag.ariteg.service

import io.ipfs.multihash.Multihash
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.util.concurrent.CountDownLatch
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import kotlin.random.Random

@SpringBootTest
@ActiveProfiles("test-real")
@Disabled
internal class DistributedLockPerformanceTest {

    @Autowired
    lateinit var distributedLockService: DistributedLockService

    private val threadCount = 1024
    private val executorService = ThreadPoolExecutor(
        threadCount, threadCount,
        10L, TimeUnit.MINUTES,
        LinkedBlockingQueue()
    )

    @AfterEach
    internal fun tearDown() {
        this.executorService.shutdownNow()
    }

    private fun int2ByteArray(data: Int): ByteArray {
        val buffer = ByteArray(Int.SIZE_BYTES) // normally should be 4
        // not the best performance, but shouldn't impact the result
        buffer[0] = (data shr 0).toByte()
        buffer[1] = (data shr 8).toByte()
        buffer[2] = (data shr 16).toByte()
        buffer[3] = (data shr 24).toByte()
        return buffer
    }

    // around 1.8 locks/ms
    @Test
    fun testParallelPerformance() {
        val totalLockCount = 16384
        val countDownLatch = CountDownLatch(totalLockCount)
        val multihashList = (0 until totalLockCount).map { Multihash(Multihash.Type.id, int2ByteArray(it)) }
        println("Preparation finished, ready to start")
        val startTime = System.currentTimeMillis()
        multihashList.forEach {
            executorService.submit {
                val lock = distributedLockService.getLock(it)
                distributedLockService.lock(lock)
                countDownLatch.countDown()
                distributedLockService.unlock(lock)
            }
        }
        while (!countDownLatch.await(5, TimeUnit.SECONDS)) {
            println("Remain: " + countDownLatch.count)
        }
        val endTime = System.currentTimeMillis()
        val timeDelta = endTime - startTime
        println("Perform $totalLockCount actions in $timeDelta ms")
        println("Average performance: ${totalLockCount / timeDelta.toDouble()} locks/ms")
        Thread.sleep(5000) // wait all client close...
    }

    @Test
    fun testRacingPerformance() {
        val totalLockCount = 200
        val countDownLatch = CountDownLatch(totalLockCount)
        val lockKey = Random.nextInt(900000, Int.MAX_VALUE)
        println("Using random lock key: $lockKey")
        val multihashList = (0 until totalLockCount).map {
            Multihash(Multihash.Type.id, int2ByteArray(lockKey))
        }
        println("Preparation finished, ready to start")
        val startTime = System.currentTimeMillis()
        multihashList.forEach {
            executorService.submit {
                val lock = distributedLockService.getLock(it)
                distributedLockService.lock(lock)
                countDownLatch.countDown()
                distributedLockService.unlock(lock)
            }
        }
        while (!countDownLatch.await(5, TimeUnit.SECONDS)) {
            println("Remain: " + countDownLatch.count)
        }
        val endTime = System.currentTimeMillis()
        val timeDelta = endTime - startTime
        println("Perform $totalLockCount actions in $timeDelta ms")
        println("Average performance: ${totalLockCount / (timeDelta / 1000.0)} locks/s")
        Thread.sleep(5000) // wait all client close...
    }
}
