package com.event_registration.lk.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BookingRequest {

    @NotBlank(message = "Event ID is required")
    private String eventId;

    @NotNull(message = "User ID is required")
    private Long userId;

    @NotNull(message = "Booking date-time is required")
    private LocalDateTime localDateTime;

    @Min(value = 1, message = "At least 1 seat must be requested")
    @NotNull(message = "Requested seats is required")
    @Builder.Default
    private Integer requestedSeats = 1;
}
