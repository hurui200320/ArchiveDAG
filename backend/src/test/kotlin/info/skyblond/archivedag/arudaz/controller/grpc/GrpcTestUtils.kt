package info.skyblond.archivedag.arudaz.controller.grpc

import info.skyblond.archivedag.arudaz.protos.common.Empty
import io.grpc.internal.testing.StreamRecorder
import org.junit.jupiter.api.Assertions

fun checkEmptyResponse(responseObserver: StreamRecorder<Empty>) {
    Assertions.assertNull(responseObserver.error)
    val results = responseObserver.values
    Assertions.assertEquals(1, results.size)
    Assertions.assertEquals(Empty.getDefaultInstance(), results[0])
}
