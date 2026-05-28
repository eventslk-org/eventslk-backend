package com.event_registration.lk.service.impl;

import com.event_registration.lk.dto.BookingOrder;
import com.event_registration.lk.dto.request.BookingRequest;
import com.event_registration.lk.dto.response.BookingResponse;
import com.event_registration.lk.entity.BookingOrderEntity;
import com.event_registration.lk.entity.EventEntity;
import com.event_registration.lk.entity.OutboxEvent;
import com.event_registration.lk.entity.UserEntity;
import com.event_registration.lk.exception.CancellationPolicyViolationException;
import com.event_registration.lk.exception.SeatsExceededException;
import com.event_registration.lk.kafka.BookingCancelledEvent;
import com.event_registration.lk.kafka.BookingConfirmedEvent;
import com.event_registration.lk.kafka.NotificationProducer;
import com.event_registration.lk.repository.BookingRepository;
import com.event_registration.lk.repository.EventRepository;
import com.event_registration.lk.repository.OutboxEventRepository;
import com.event_registration.lk.repository.UserRepository;
import com.event_registration.lk.service.BookingService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@Primary
public class BookingServiceImpl implements BookingService {

    private static final int CANCELLATION_CUTOFF_DAYS = 3;

    private final BookingRepository bookingRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    private final NotificationProducer notificationProducer;

    public BookingServiceImpl(BookingRepository bookingRepository,
                              EventRepository eventRepository,
                              UserRepository userRepository,
                              OutboxEventRepository outboxEventRepository,
                              ObjectMapper objectMapper,
                              NotificationProducer notificationProducer) {
        this.bookingRepository = bookingRepository;
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
        this.notificationProducer = notificationProducer;
    }

    /**
     * Books seats for an event.
     *
     * Concurrency strategy: PESSIMISTIC_WRITE lock on the event row ensures that
     * concurrent booking requests are serialized at the DB level.  Only one
     * transaction can hold the lock at a time, so the check-then-decrement is atomic
     * without requiring application-level retries (contrast with optimistic locking,
     * which would throw ObjectOptimisticLockingFailureException under high contention
     * and force the caller to retry — undesirable for a ticketing workload).
     *
     * The @Version field on EventEntity still guards admin-level metadata updates
     * (name, description changes) which are low-frequency and tolerate retries.
     *
     * If availableSeats reaches 0 a SEATS_FULL OutboxEvent is written inside the
     * same transaction, guaranteeing exactly-once Kafka delivery via the OutboxPoller.
     */
    @Override
    @Transactional
    public BookingResponse bookEvent(BookingRequest bookingRequest) {
        int requestedSeats = bookingRequest.getRequestedSeats() != null
                ? bookingRequest.getRequestedSeats() : 1;

        // ── 1. Acquire pessimistic lock on event row ───────────────────────
        EventEntity event = eventRepository
                .findByEventIdForUpdate(bookingRequest.getEventId())
                .orElse(null);
        if (event == null) {
            return BookingResponse.builder()
                    .status("booking")
                    .message("unsuccess: event not found")
                    .build();
        }

        // ── 2. Validate user ───────────────────────────────────────────────
        Optional<UserEntity> userOpt = userRepository.findById(bookingRequest.getUserId());
        if (userOpt.isEmpty()) {
            return BookingResponse.builder()
                    .status("booking")
                    .message("unsuccess: user not found")
                    .build();
        }

        // ── 3. Seat availability check (throws → HTTP 400 via GlobalExceptionHandler) ──
        if (event.getAvailableSeats() < requestedSeats) {
            throw new SeatsExceededException(requestedSeats, event.getAvailableSeats());
        }

        // ── 4. Atomically decrement seats ──────────────────────────────────
        event.setAvailableSeats(event.getAvailableSeats() - requestedSeats);

        // ── 5. Persist booking record ──────────────────────────────────────
        String bookingId = generateBookingId();
        BookingOrderEntity bookingEntity = BookingOrderEntity.builder()
                .bookingId(bookingId)
                .eventId(event.getEventId())
                .userId(bookingRequest.getUserId())
                .ticketNumber(generateTicketNumber())
                .orderedDate(bookingRequest.getLocalDateTime())
                .orderStatus("confirmed")
                .seatCount(requestedSeats)
                .build();
        bookingRepository.save(bookingEntity);

        // ── 6. Seats-full outbox event (same transaction = guaranteed delivery) ──
        if (event.getAvailableSeats() == 0) {
            publishSeatsFullOutbox(event);
            log.info("[booking] event {} is now SOLD OUT", event.getEventId());
        }

        // ── 7. Booking-confirmed Kafka notification (best-effort, outside tx boundary) ──
        UserEntity user = userOpt.get();
        String eventDate = formatFirstDate(event);
        try {
            notificationProducer.publishBookingConfirmed(BookingConfirmedEvent.builder()
                    .email(user.getEmail())
                    .username(user.getUsername())
                    .eventName(event.getName())
                    .ticketNumber(bookingEntity.getTicketNumber())
                    .eventDate(eventDate)
                    .bookingId(bookingId)
                    .build());
        } catch (Exception e) {
            log.warn("[booking] notification publish failed for bookingId={}: {}", bookingId, e.getMessage());
        }

        log.info("[booking] confirmed bookingId={} seats={} eventId={}",
                bookingId, requestedSeats, event.getEventId());

        BookingOrder order = toDto(bookingEntity);
        ArrayList<BookingOrder> result = new ArrayList<>();
        result.add(order);
        return BookingResponse.builder()
                .status("booking")
                .message("success")
                .orderList(result)
                .build();
    }

    /**
     * Cancels a booking, enforcing the 3-day cancellation policy.
     *
     * The booking status is set to "cancelled" (soft-delete) so the audit trail
     * is preserved.  Seats are restored to the event within the same transaction.
     */
    @Override
    @Transactional
    public BookingResponse cancelBooking(String bookingId) {
        // ── 1. Load booking ────────────────────────────────────────────────
        Optional<BookingOrderEntity> bookingOpt = bookingRepository.findById(bookingId);
        if (bookingOpt.isEmpty()) {
            return BookingResponse.builder()
                    .status("booking-cancel")
                    .message("no record exists for booking id: " + bookingId)
                    .build();
        }
        BookingOrderEntity booking = bookingOpt.get();

        if ("cancelled".equalsIgnoreCase(booking.getOrderStatus())) {
            return BookingResponse.builder()
                    .status("booking-cancel")
                    .message("booking is already cancelled")
                    .build();
        }

        // ── 2. Acquire pessimistic lock on event ───────────────────────────
        EventEntity event = eventRepository
                .findByEventIdForUpdate(booking.getEventId())
                .orElse(null);

        // ── 3. Enforce 3-day cancellation policy ──────────────────────────
        if (event != null && event.getDates() != null && !event.getDates().isEmpty()) {
            LocalDateTime earliestDate = event.getDates().stream()
                    .min(LocalDateTime::compareTo)
                    .orElse(null);
            if (earliestDate != null) {
                long hoursUntilEvent = ChronoUnit.HOURS.between(LocalDateTime.now(), earliestDate);
                if (hoursUntilEvent < CANCELLATION_CUTOFF_DAYS * 24L) {
                    throw new CancellationPolicyViolationException(bookingId, hoursUntilEvent);
                }
            }
        }

        // ── 4. Restore seats ───────────────────────────────────────────────
        if (event != null) {
            int restored = Math.min(booking.getSeatCount(), event.getTotalSeats());
            event.setAvailableSeats(Math.min(event.getAvailableSeats() + restored, event.getTotalSeats()));
        }

        // ── 5. Soft-delete: mark booking cancelled ─────────────────────────
        booking.setOrderStatus("cancelled");

        // ── 6. Best-effort cancellation notification ───────────────────────
        Optional<UserEntity> userOpt = userRepository.findById(booking.getUserId());
        if (userOpt.isPresent() && event != null) {
            try {
                notificationProducer.publishBookingCancelled(BookingCancelledEvent.builder()
                        .email(userOpt.get().getEmail())
                        .username(userOpt.get().getUsername())
                        .eventName(event.getName())
                        .bookingId(bookingId)
                        .build());
            } catch (Exception e) {
                log.warn("[cancel] notification failed for bookingId={}: {}", bookingId, e.getMessage());
            }
        }

        log.info("[booking-cancel] cancelled bookingId={} seats-restored={}", bookingId, booking.getSeatCount());
        return BookingResponse.builder()
                .status("booking-cancel")
                .message("success")
                .build();
    }

    @Override
    public BookingResponse getUserBookingDetails(Long userId) {
        List<BookingOrderEntity> entities = bookingRepository.findAllByUserId(userId);
        if (entities.isEmpty()) {
            return BookingResponse.builder()
                    .status("booking-details")
                    .message("no record exists for user id: " + userId)
                    .build();
        }
        List<BookingOrder> orders = entities.stream().map(this::toDto).collect(Collectors.toList());
        return BookingResponse.builder()
                .status("booking-details")
                .message("record found for user id: " + userId)
                .orderList((ArrayList<BookingOrder>) orders)
                .build();
    }

    // ── private helpers ───────────────────────────────────────────────────────

    private void publishSeatsFullOutbox(EventEntity event) {
        try {
            String payload = objectMapper.writeValueAsString(Map.of(
                    "eventId", event.getEventId(),
                    "eventName", event.getName(),
                    "totalSeats", event.getTotalSeats(),
                    "soldOutAt", LocalDateTime.now().toString()
            ));
            OutboxEvent outbox = OutboxEvent.builder()
                    .aggregateType("Event")
                    .aggregateId(event.getEventId())
                    .type("SEATS_FULL")
                    .payload(payload)
                    .status(OutboxEvent.Status.PENDING)
                    .build();
            outboxEventRepository.save(outbox);
        } catch (JsonProcessingException e) {
            log.error("[outbox] failed to serialise SEATS_FULL payload for event={}: {}",
                    event.getEventId(), e.getMessage());
        }
    }

    private BookingOrder toDto(BookingOrderEntity entity) {
        return BookingOrder.builder()
                .bookingId(entity.getBookingId())
                .eventId(entity.getEventId())
                .userId(entity.getUserId())
                .ticketNumber(entity.getTicketNumber())
                .orderDate(entity.getOrderedDate())
                .orderStatus(entity.getOrderStatus())
                .build();
    }

    private String formatFirstDate(EventEntity event) {
        if (event.getDates() == null || event.getDates().isEmpty()) return "See event details";
        return event.getDates().get(0).format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm"));
    }

    private String generateTicketNumber() {
        LocalDateTime now = LocalDateTime.now();
        String date = now.format(DateTimeFormatter.ofPattern("ddMMyyyy"));
        String time = now.format(DateTimeFormatter.ofPattern("ssmmHH"));
        long todayCount = bookingRepository.countByOrderedDateBetween(
                now.toLocalDate().atStartOfDay(),
                now.toLocalDate().plusDays(1).atStartOfDay());
        return "TKT" + String.format("%05d", todayCount) + time + date;
    }

    private String generateBookingId() {
        return "B" + UUID.randomUUID().toString().replace("-", "").substring(0, 9).toUpperCase();
    }
}
