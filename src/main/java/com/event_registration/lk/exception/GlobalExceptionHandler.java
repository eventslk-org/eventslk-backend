package com.event_registration.lk.exception;

import jakarta.persistence.OptimisticLockException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;

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
}
