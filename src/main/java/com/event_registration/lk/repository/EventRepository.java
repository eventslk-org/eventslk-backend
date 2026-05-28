package com.event_registration.lk.repository;

import com.event_registration.lk.entity.EventEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface EventRepository extends JpaRepository<EventEntity, String> {

    EventEntity findEventEntityByEventIdContainingIgnoreCase(String eventId);
    Boolean existsEventEntityByEventIdContainingIgnoreCase(String eventId);

    // Used exclusively during bookEvent / cancelBooking to serialize concurrent seat changes.
    // PESSIMISTIC_WRITE issues SELECT ... FOR UPDATE, ensuring only one transaction at a time
    // can modify availableSeats for a given event row.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM EventEntity e WHERE e.eventId = :eventId")
    Optional<EventEntity> findByEventIdForUpdate(@Param("eventId") String eventId);
}
