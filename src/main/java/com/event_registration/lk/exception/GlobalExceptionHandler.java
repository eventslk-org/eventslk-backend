package com.event_registration.lk.exception;

import jakarta.persistence.OptimisticLockException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(SeatsExceededException.class)
    public ProblemDetail handleSeatsExceeded(SeatsExceededException ex) {
        log.warn("[seat-check] {}", ex.getMessage());
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        detail.setType(URI.create("https://eventslk.lk/errors/seats-exceeded"));
        detail.setTitle("Insufficient Seats");
        detail.setProperty("requested", ex.getRequested());
        detail.setProperty("available", ex.getAvailable());
        return detail;
    }

    @ExceptionHandler(CancellationPolicyViolationException.class)
    public ProblemDetail handleCancellationPolicy(CancellationPolicyViolationException ex) {
        log.warn("[cancel-policy] {}", ex.getMessage());
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        detail.setType(URI.create("https://eventslk.lk/errors/cancellation-policy-violation"));
        detail.setTitle("Cancellation Not Allowed");
        return detail;
    }

    // Thrown when two concurrent updates collide on @Version (admin-level event edits).
    @ExceptionHandler({OptimisticLockException.class, ObjectOptimisticLockingFailureException.class})
    public ProblemDetail handleOptimisticLock(RuntimeException ex) {
        log.warn("[optimistic-lock] concurrent update conflict: {}", ex.getMessage());
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                "The record was modified by another request. Please refresh and retry.");
        detail.setType(URI.create("https://eventslk.lk/errors/concurrent-modification"));
        detail.setTitle("Concurrent Modification Conflict");
        return detail;
    }

    /** Bean-validation failures (@Valid) → 400 with a field→message map. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(fe -> fieldErrors.putIfAbsent(fe.getField(), fe.getDefaultMessage()));
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Request validation failed.");
        detail.setType(URI.create("https://eventslk.lk/errors/validation"));
        detail.setTitle("Validation Failed");
        detail.setProperty("errors", fieldErrors);
        return detail;
    }

    /** Bad credentials / disabled account raised from the login flow → 401. */
    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail handleAuthentication(AuthenticationException ex) {
        log.warn("[auth] authentication failed: {}", ex.getMessage());
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED, "Invalid email or password.");
        detail.setType(URI.create("https://eventslk.lk/errors/invalid-credentials"));
        detail.setTitle("Authentication Failed");
        return detail;
    }

    /** Deliberate HTTP errors thrown from controllers keep their status. */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ProblemDetail> handleResponseStatus(ResponseStatusException ex) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(
                HttpStatus.valueOf(ex.getStatusCode().value()), ex.getReason());
        return ResponseEntity.status(ex.getStatusCode()).body(detail);
    }

    /** Last-resort handler: log the full stack trace, expose none of it. */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex) {
        log.error("[unhandled] {}", ex.getMessage(), ex);
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred. Please try again later.");
        detail.setType(URI.create("https://eventslk.lk/errors/internal"));
        detail.setTitle("Internal Server Error");
        return detail;
    }
}
