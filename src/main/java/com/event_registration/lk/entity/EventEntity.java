package com.event_registration.lk.entity;

import com.event_registration.lk.dto.PriceRange;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventEntity {

    @ElementCollection
    @CollectionTable(
            name = "event_price_ranges",
            joinColumns = @JoinColumn(name = "event_id") //add relations to db
    )
    //private ArrayList<PriceRange> priceRanges;
    private List<PriceRange> priceRanges;
    @Id
    private String eventId;
    private String name;
    private String description;
    private ArrayList<LocalDateTime> dates;
    private String location;
    @Lob //convert into BLOB object....byte[] maps to BLOB in databases
    @Column(name = "image_data")
    private byte[] image;
}
