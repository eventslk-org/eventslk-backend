package com.event_registration.lk.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

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
    private String orderStatus; //confirm or canceled

}
