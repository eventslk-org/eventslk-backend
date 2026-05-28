package com.event_registration.lk.exception;

public class CancellationPolicyViolationException extends RuntimeException {

    public CancellationPolicyViolationException(String bookingId, long hoursUntilEvent) {
        super(String.format(
                "Booking '%s' cannot be cancelled: the event starts in %d hour(s), " +
                "which is within the 3-day cancellation window.",
                bookingId, hoursUntilEvent));
    }
}
