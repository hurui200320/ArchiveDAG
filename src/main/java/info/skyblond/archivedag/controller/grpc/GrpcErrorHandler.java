package info.skyblond.archivedag.controller.grpc;

import info.skyblond.archivedag.model.DuplicatedEntityException;
import info.skyblond.archivedag.model.EntityNotFoundException;
import info.skyblond.archivedag.model.PermissionDeniedException;
import io.grpc.Status;
import kotlin.NotImplementedError;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.advice.GrpcAdvice;
import net.devh.boot.grpc.server.advice.GrpcExceptionHandler;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;

@Slf4j
@GrpcAdvice
public class GrpcErrorHandler {
    // TODO test this in integration test
    // CANCELLED - 1
    // UNKNOWN - 2

    // INVALID_ARGUMENT - 3
    @GrpcExceptionHandler({
            IllegalArgumentException.class,
    })
    public Status handleInvalidArgument(Throwable t) {
        return Status.INVALID_ARGUMENT
                .withDescription(t.getMessage())
                .withCause(t);
    }

    // DEADLINE_EXCEEDED - 4

    // NOT_FOUND - 5
    @GrpcExceptionHandler({
            EntityNotFoundException.class,
    })
    public Status handleNotFound(Throwable t) {
        return Status.NOT_FOUND
                .withDescription(t.getMessage())
                .withCause(t);
    }

    // ALREADY_EXISTS - 6
    @GrpcExceptionHandler({
            DuplicatedEntityException.class,
    })
    public Status handleAlreadyExists(Throwable t) {
        return Status.ALREADY_EXISTS
                .withDescription(t.getMessage())
                .withCause(t);
    }

    // PERMISSION_DENIED - 7
    @GrpcExceptionHandler({
            AccessDeniedException.class,
            PermissionDeniedException.class,
            LockedException.class,
            DisabledException.class,
    })
    public Status handlePermissionDenied(Throwable t) {
        return Status.PERMISSION_DENIED
                .withDescription(t.getMessage())
                .withCause(t);
    }

    // RESOURCE_EXHAUSTED - 8
    // FAILED_PRECONDITION - 9
    // ABORTED - 10
    // OUT_OF_RANGE - 11

    // UNIMPLEMENTED - 12
    @GrpcExceptionHandler({
            NotImplementedError.class,
    })
    public Status handleUnimplemented(Throwable t) {
        return Status.UNIMPLEMENTED
                .withDescription(t.getMessage())
                .withCause(t);
    }

    // INTERNAL - 13
    @GrpcExceptionHandler(Throwable.class)
    public Status handleInternal(Throwable t) {
        log.error("Unexpected error", t);
        return Status.INTERNAL
                .withDescription(t.getMessage())
                .withCause(t);
    }

    // UNAVAILABLE - 14
    // DATA_LOSS - 15

    // UNAUTHENTICATED - 16
    @GrpcExceptionHandler({
            BadCredentialsException.class,
    })
    public Status handleUnauthenticated(Throwable t) {
        return Status.UNAUTHENTICATED
                .withDescription(t.getMessage())
                .withCause(t);
    }
}
