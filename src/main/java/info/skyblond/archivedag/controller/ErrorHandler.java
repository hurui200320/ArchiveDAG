package info.skyblond.archivedag.controller;

import info.skyblond.archivedag.model.DuplicatedEntityException;
import info.skyblond.archivedag.model.EntityNotFoundException;
import info.skyblond.archivedag.model.ExceptionResponse;
import info.skyblond.archivedag.model.PermissionDeniedException;
import kotlin.NotImplementedError;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.servlet.http.HttpServletRequest;

@Slf4j
@RestControllerAdvice
public class ErrorHandler {
    // TODO What about gRPC exception?

    // 400 - Bad Request
    @ExceptionHandler({
            IllegalArgumentException.class,
            HttpMessageNotReadableException.class,
    })
    @ResponseBody()
    public ResponseEntity<ExceptionResponse> handleBadRequest(HttpServletRequest request, Throwable t) {
        return ExceptionResponse.generateResp(HttpStatus.BAD_REQUEST, request, t);
    }

    // 401 - Unauthorized
    @ExceptionHandler({
            BadCredentialsException.class,
    })
    @ResponseBody()
    public ResponseEntity<ExceptionResponse> handleUnauthorized(HttpServletRequest request, Throwable e) {
        return ExceptionResponse.generateResp(HttpStatus.UNAUTHORIZED, request, e);
    }

    // 402 - Payment Required

    // 403 - Forbidden
    @ExceptionHandler({
            AccessDeniedException.class,
            PermissionDeniedException.class,
            LockedException.class,
            DisabledException.class,
    })
    @ResponseBody()
    public ResponseEntity<ExceptionResponse> handleForbidden(HttpServletRequest request, Throwable t) {
        return ExceptionResponse.generateResp(HttpStatus.FORBIDDEN, request, t);
    }

    // 404 - Not Found
    @ExceptionHandler({
            EntityNotFoundException.class,
    })
    @ResponseBody()
    public ResponseEntity<ExceptionResponse> handleNotFound(HttpServletRequest request, Throwable t) {
        return ExceptionResponse.generateResp(HttpStatus.NOT_FOUND, request, t);
    }

    // 405 - Method Not Allowed
    @ExceptionHandler({
            HttpRequestMethodNotSupportedException.class,
    })
    @ResponseBody()
    public ResponseEntity<ExceptionResponse> handleMethodNotAllowed(HttpServletRequest request, Throwable t) {
        return ExceptionResponse.generateResp(HttpStatus.METHOD_NOT_ALLOWED, request, t);
    }

    // 406 - Not Acceptable
    @ExceptionHandler({
            HttpMediaTypeNotAcceptableException.class,
    })
    @ResponseBody()
    public ResponseEntity<ExceptionResponse> handleNotAcceptable(HttpServletRequest request, Throwable t) {
        return ExceptionResponse.generateResp(HttpStatus.NOT_ACCEPTABLE, request, t);
    }

    // 407 - Proxy Authentication Required
    // 408 - Request Timeout

    // 409 - Conflict
    @ExceptionHandler({
            DuplicatedEntityException.class,
    })
    @ResponseBody()
    public ResponseEntity<ExceptionResponse> handleConflict(HttpServletRequest request, Throwable t) {
        return ExceptionResponse.generateResp(HttpStatus.CONFLICT, request, t);
    }

    // 410 - Gone
    // 411 - Length Required
    // 412 - Precondition Failed
    // 413 - Payload Too Large
    // 413 - Request Entity Too Large
    // 414 - URI Too Long
    // 414 - Request-URI Too Long

    // 415 - Unsupported MediaType
    @ExceptionHandler({
            HttpMediaTypeNotSupportedException.class,
    })
    @ResponseBody()
    public ResponseEntity<ExceptionResponse> handleUnsupportedMediaType(HttpServletRequest request, Throwable t) {
        return ExceptionResponse.generateResp(HttpStatus.UNSUPPORTED_MEDIA_TYPE, request, t);
    }

    // 416 - Requested range not satisfiable
    // 417 - Expectation Failed
    // 418 - I'm a teapot
    // 419 - Insufficient Space On Resource
    // 420 - Method Failure
    // 421 - Destination Locked
    // 422 - Unprocessable Entity
    // 423 - Locked
    // 424 - Failed Dependency
    // 425 - Too Early
    // 426 - Upgrade Required
    // 428 - Precondition Required
    // 429 - Too Many Requests
    // 431 - Request Header Fields Too Large
    // 451 - Unavailable For Legal Reasons

    // 500 - Internal Server Error
    @ExceptionHandler(Throwable.class)
    @ResponseBody()
    public ResponseEntity<ExceptionResponse> handleInternalServerError(HttpServletRequest request, Throwable t) {
        log.error("Unexpected error", t);
        return ExceptionResponse.generateResp(HttpStatus.INTERNAL_SERVER_ERROR, request, t);
    }

    // 501 - Not Implemented
    @ExceptionHandler({
            NotImplementedError.class,
    })
    @ResponseBody()
    public ResponseEntity<ExceptionResponse> handleNotImplemented(HttpServletRequest request, Throwable t) {
        return ExceptionResponse.generateResp(HttpStatus.NOT_IMPLEMENTED, request, t);
    }


    // 502 - Bad Gateway
    // 503 - Service Unavailable
    // 504 - Gateway Timeout
    // 505 - HTTP Version not supported
    // 506 - Variant Also Negotiates
    // 507 - Insufficient Storage
    // 508 - Loop Detected
    // 509 - Bandwidth Limit Exceeded
    // 510 - Not Extended
    // 511 - Network Authentication Required
}
