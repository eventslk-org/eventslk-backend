package com.event_registration.lk.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class BookingOrderEntity {

    @Id
    private String bookingId;
    private Long userId;
    private String eventId;
    private String ticketNumber;
    private LocalDateTime orderedDate;
    private String orderStatus;

    // Number of seats reserved; needed to restore availableSeats on cancellation.
    @Column(name = "seat_count", nullable = false)
    private int seatCount;
}
