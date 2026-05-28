package com.event_registration.lk.entity;

import com.event_registration.lk.dto.PriceRange;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "event_entity")
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventEntity {

    @Id
    private String eventId;

    private String name;
    private String description;
    private String location;

    // S3/CDN URL set by the client after direct upload via presigned URL.
    @Column(name = "image_url", length = 1024)
    private String imageUrl;

    @Column(name = "total_seats", nullable = false)
    private int totalSeats;

    // Decremented atomically on booking; incremented on cancellation.
    @Column(name = "available_seats", nullable = false)
    private int availableSeats;

    // Guards concurrent writes; used for update operations (name/desc changes).
    // Seat decrements are protected by PESSIMISTIC_WRITE lock instead (see EventRepository).
    @Version
    @Column(name = "version", nullable = false)
    private Integer version;

    private ArrayList<LocalDateTime> dates;

    @ElementCollection
    @CollectionTable(
            name = "event_price_ranges",
            joinColumns = @JoinColumn(name = "event_id")
    )
    private List<PriceRange> priceRanges;
}
