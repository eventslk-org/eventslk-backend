package com.event_registration.lk.service;

import com.event_registration.lk.dto.request.BookingRequest;
import com.event_registration.lk.dto.response.BookingResponse;

public interface BookingService {

    public BookingResponse bookEvent(BookingRequest bookingRequest);
    public BookingResponse cancelBooking(String bookingId);
    public BookingResponse getUserBookingDetails(Long userId);
}
