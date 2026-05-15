package com.event_registration.lk.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingConfirmedEvent {
    private String email;
    private String username;
    private String eventName;
    private String ticketNumber;
    private String eventDate;
    private String bookingId;
}
