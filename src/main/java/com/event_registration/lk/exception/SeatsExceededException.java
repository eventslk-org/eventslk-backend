package com.event_registration.lk.exception;

public class SeatsExceededException extends RuntimeException {

    private final int requested;
    private final int available;

    public SeatsExceededException(int requested, int available) {
        super(String.format("Cannot book %d seat(s): only %d seat(s) available", requested, available));
        this.requested = requested;
        this.available = available;
    }

    public int getRequested() { return requested; }
    public int getAvailable() { return available; }
}
