package com.event_registration.lk.repository;

import com.event_registration.lk.entity.BookingOrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface BookingRepository extends JpaRepository <BookingOrderEntity,String> {
    boolean existsByBookingIdContainingIgnoreCase(String bookingId);
    boolean existsByBookingId(String bookingId);
    long countByOrderedDateBetween(LocalDateTime start, LocalDateTime end);
    List<BookingOrderEntity> findAllByUserId(Long userId);
}
