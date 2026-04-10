package com.event_registration.lk.dto.request;

import com.event_registration.lk.dto.Event;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
@Builder
@AllArgsConstructor
public class BookingRequest {
    @NotBlank(message = "Event ID is required")
    private String eventId;
    
    @NotNull(message = "User ID is required")
    private Long userId;
    
    @NotNull(message = "Local Date Time is required")
    private LocalDateTime localDateTime;
}
