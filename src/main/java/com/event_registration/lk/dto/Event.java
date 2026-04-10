package com.event_registration.lk.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
@AllArgsConstructor
@Builder
@NoArgsConstructor
public class Event {
    @JsonProperty("eventId")
    private String eventId;

    @NotBlank(message = "Name is required")
    @JsonProperty("name")
    private String name;

    @NotBlank(message = "Description is required")
    @JsonProperty("description")
    private String description;

    @JsonProperty("priceRanges")
    private List<PriceRange> priceRanges;

    @NotNull(message = "Dates are required")
    @JsonProperty("dates")
    private ArrayList<LocalDateTime> dates;

    @NotBlank(message = "Location is required")
    @JsonProperty("location")
    private String location;

    @JsonProperty("image")
    private byte[] image;
}
