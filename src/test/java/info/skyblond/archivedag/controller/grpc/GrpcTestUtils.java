package info.skyblond.archivedag.controller.grpc;

import info.skyblond.archivedag.protos.common.Empty;
import io.grpc.internal.testing.StreamRecorder;
import org.junit.jupiter.api.function.Executable;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class GrpcTestUtils {
    public static void checkEmptyResponse(StreamRecorder<Empty> responseObserver) {
        assertNull(responseObserver.getError());
        List<Empty> results = responseObserver.getValues();
        assertEquals(1, results.size());
        assertEquals(Empty.getDefaultInstance(), results.get(0));
    }

    public static void safeExecutable(Executable executable) {
        try {
            executable.execute();
        } catch (Throwable ignored) {
        }
    }
}
