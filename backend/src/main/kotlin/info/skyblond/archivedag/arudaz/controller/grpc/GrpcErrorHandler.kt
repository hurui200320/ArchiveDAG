package info.skyblond.archivedag.arudaz.controller.grpc

import info.skyblond.archivedag.commons.DuplicatedEntityException
import info.skyblond.archivedag.commons.EntityNotFoundException
import info.skyblond.archivedag.commons.PermissionDeniedException
import io.grpc.Status
import net.devh.boot.grpc.server.advice.GrpcAdvice
import net.devh.boot.grpc.server.advice.GrpcExceptionHandler
import org.slf4j.LoggerFactory
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.DisabledException
import org.springframework.security.authentication.LockedException

@Suppress("unused")
@GrpcAdvice
class GrpcErrorHandler {
    private val logger = LoggerFactory.getLogger(GrpcErrorHandler::class.java)

    // CANCELLED - 1
    // UNKNOWN - 2

    // INVALID_ARGUMENT - 3
    @GrpcExceptionHandler(IllegalArgumentException::class)
    fun handleInvalidArgument(t: Throwable): Status {
        return Status.INVALID_ARGUMENT
            .withDescription(t.message)
            .withCause(t)
    }

    // DEADLINE_EXCEEDED - 4

    // NOT_FOUND - 5
    @GrpcExceptionHandler(EntityNotFoundException::class)
    fun handleNotFound(t: Throwable): Status {
        return Status.NOT_FOUND
            .withDescription(t.message)
            .withCause(t)
    }

    // ALREADY_EXISTS - 6
    @GrpcExceptionHandler(DuplicatedEntityException::class)
    fun handleAlreadyExists(t: Throwable): Status {
        return Status.ALREADY_EXISTS
            .withDescription(t.message)
            .withCause(t)
    }

    // PERMISSION_DENIED - 7
    @GrpcExceptionHandler(
        AccessDeniedException::class,
        PermissionDeniedException::class,
        LockedException::class,
        DisabledException::class
    )
    fun handlePermissionDenied(t: Throwable): Status {
        return Status.PERMISSION_DENIED
            .withDescription(t.message)
            .withCause(t)
    }

    // RESOURCE_EXHAUSTED - 8

    // FAILED_PRECONDITION - 9
    @GrpcExceptionHandler(IllegalStateException::class)
    fun handleFailedPrecondition(t: Throwable): Status {
        return Status.FAILED_PRECONDITION
            .withDescription(t.message)
            .withCause(t)
    }

    // ABORTED - 10
    // OUT_OF_RANGE - 11

    // UNIMPLEMENTED - 12
    @GrpcExceptionHandler(NotImplementedError::class)
    fun handleUnimplemented(t: Throwable): Status {
        return Status.UNIMPLEMENTED
            .withDescription(t.message)
            .withCause(t)
    }

    // INTERNAL - 13
    @GrpcExceptionHandler(Throwable::class)
    fun handleInternal(t: Throwable): Status {
        logger.error("Unexpected error", t)
        return Status.INTERNAL
            .withDescription(t.message)
            .withCause(t)
    }

    // UNAVAILABLE - 14
    // DATA_LOSS - 15

    // UNAUTHENTICATED - 16
    @GrpcExceptionHandler(BadCredentialsException::class)
    fun handleUnauthenticated(t: Throwable): Status {
        return Status.UNAUTHENTICATED
            .withDescription(t.message)
            .withCause(t)
    }
}
