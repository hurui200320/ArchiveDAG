package info.skyblond.archivedag.ariteg

import com.google.protobuf.ByteString
import info.skyblond.archivedag.ariteg.model.BlobObject
import info.skyblond.archivedag.ariteg.model.ListObject
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import kotlin.random.Random

@SpringBootTest
@ActiveProfiles("test-real")
@Disabled
internal class AritegPerformanceTest {
    @Autowired
    lateinit var aritegService: AritegService

    @BeforeEach
    internal fun setUp() {
        System.gc()
    }

    private fun testFilePerformance(fileSize: Int, fileCount: Int) {
        val contents = (0 until fileCount)
            .map { ByteString.copyFrom(Random.nextBytes(fileSize)) }
            .map { BlobObject(it) }
        println("Prepare done, ready to start")
        val startTime = System.currentTimeMillis()
        contents.parallelStream()
            .map { aritegService.writeProto("", it).completionFuture }
            .forEach { it.get() }
        val endTime = System.currentTimeMillis()
        val timeDelta = endTime - startTime
        println("Writing $fileCount*${fileSize}B in $timeDelta ms")
        println("Average performance: ${fileCount / timeDelta.toDouble()} blobs/ms")
        Thread.sleep(5000) // wait all client close...
    }

    @Test
    fun testSmallFilePerformance() {
        val fileSize = 1024 // 1KB
        val fileCount = 16384 // total 16MB

        testFilePerformance(fileSize, fileCount)
    }

    @Test
    fun testMiddleFilePerformance() {
        val fileSize = 256 * 1024 // 256KB
        val fileCount = 8192 // total 2GB

        testFilePerformance(fileSize, fileCount)
    }

    @Test
    fun testBigFilePerformance() {
        val fileSize = 2 * 1024 * 1024 // 2MB
        val fileCount = 1024 + 512 + 256 // total 4GB

        testFilePerformance(fileSize, fileCount)
    }

    private fun testListPerformance(listSize: Int, listCount: Int) {
        var blobs = (0 until listSize)
            .map {
                val b = BlobObject(ByteString.copyFrom(Random.nextBytes(1024)))
                aritegService.writeProto("", b)
            }
            .map {
                it.completionFuture.get()
                it.link
            }
        System.gc()
        val contents = mutableListOf<ListObject>()
        while (blobs.isNotEmpty()) {
            val listObj = ListObject(blobs.take(listSize))
            blobs = blobs.drop(listSize)
            contents.add(listObj)
        }

        println("Prepare done, ready to start")
        val startTime = System.currentTimeMillis()
        contents.parallelStream()
            .map { aritegService.writeProto("", it).completionFuture }
            .forEach { it.get() }
        val endTime = System.currentTimeMillis()
        val timeDelta = endTime - startTime
        println("Writing $listCount*${listSize} elements in $timeDelta ms")
        println("Average performance: ${listCount / timeDelta.toDouble()} lists/ms")
        Thread.sleep(5000) // wait all client close...
    }

    @Test
    fun testSmallListPerformance() {
        val listSize = 128
        val listCount = 16384

        testListPerformance(listSize, listCount)
    }

    @Test
    fun testMiddleListPerformance() {
        val listSize = 1024
        val listCount = 4096

        testListPerformance(listSize, listCount)
    }

    @Test
    fun testBigListPerformance() {
        val listSize = 16384
        val listCount = 128

        testListPerformance(listSize, listCount)
    }

}
