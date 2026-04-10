package com.event_registration.lk.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BookingOrder {

    private String bookingId;
    private Long userId;
    private String eventId;
    private String ticketNumber;
    private LocalDateTime orderDate;
    private String orderStatus; //confirm or cancelled


}
