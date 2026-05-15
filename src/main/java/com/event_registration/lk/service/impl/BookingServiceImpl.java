package com.event_registration.lk.service.impl;

import com.event_registration.lk.dto.BookingOrder;
import com.event_registration.lk.dto.request.BookingRequest;
import com.event_registration.lk.dto.response.BookingResponse;
import com.event_registration.lk.entity.BookingOrderEntity;
import com.event_registration.lk.entity.EventEntity;
import com.event_registration.lk.entity.UserEntity;
import com.event_registration.lk.kafka.BookingCancelledEvent;
import com.event_registration.lk.kafka.BookingConfirmedEvent;
import com.event_registration.lk.kafka.NotificationProducer;
import com.event_registration.lk.repository.BookingRepository;
import com.event_registration.lk.repository.EventRepository;
import com.event_registration.lk.repository.UserRepository;
import com.event_registration.lk.service.BookingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

//Done
@Slf4j
@Service
@Primary
public class BookingServiceImpl implements BookingService {

    BookingRepository bookingRepository;
    EventRepository eventRepository;
    UserRepository userRepository;
    ObjectMapper objectMapper;
    NotificationProducer notificationProducer;

    public BookingServiceImpl(BookingRepository bookingRepository, EventRepository eventRepository,
            UserRepository userRepository, ObjectMapper objectMapper,
            NotificationProducer notificationProducer) {
        this.bookingRepository = bookingRepository;
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
        this.notificationProducer = notificationProducer;
    }

    //Done
    @Override
    public BookingResponse bookEvent(BookingRequest bookingRequest) {
        try {
            if (!eventRepository.existsEventEntityByEventIdContainingIgnoreCase(bookingRequest.getEventId())) {
                return BookingResponse.builder()
                        .status("booking")
                        .message("unsuccess : event not found")
                        .build();
            }
            if (!userRepository.existsByUserId(bookingRequest.getUserId())) {
                return BookingResponse.builder()
                        .status("booking")
                        .message("unsuccess : user not found")
                        .build();
            }

            BookingOrder order = BookingOrder.builder()
                    .eventId(bookingRequest.getEventId())
                    .userId(bookingRequest.getUserId())
                    .bookingId(generateBookingId())
                    .ticketNumber(generateTicketNumber())
                    .orderDate(bookingRequest.getLocalDateTime())
                    .orderStatus("confirmed")
                    .build();

            BookingOrderEntity bookingOrderEntity = objectMapper.convertValue(order, BookingOrderEntity.class);
            bookingRepository.save(bookingOrderEntity);

            // Publish booking confirmed notification
            Optional<UserEntity> userOpt = userRepository.findById(bookingRequest.getUserId());
            EventEntity event = eventRepository.findEventEntityByEventIdContainingIgnoreCase(bookingRequest.getEventId());
            if (userOpt.isPresent() && event != null) {
                String eventDate = (event.getDates() != null && !event.getDates().isEmpty())
                        ? event.getDates().get(0).format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm"))
                        : "See event details";
                notificationProducer.publishBookingConfirmed(BookingConfirmedEvent.builder()
                        .email(userOpt.get().getEmail())
                        .username(userOpt.get().getUsername())
                        .eventName(event.getName())
                        .ticketNumber(order.getTicketNumber())
                        .eventDate(eventDate)
                        .bookingId(order.getBookingId())
                        .build());
            }

            ArrayList<BookingOrder> bookingOrderList = new ArrayList<>();
            bookingOrderList.add(order);
            return BookingResponse.builder()
                    .status("booking")
                    .message("success")
                    .orderList(bookingOrderList)
                    .build();
        } catch (Exception e) {
            log.info("error in booking event : " + e.getMessage());
            return BookingResponse.builder()
                    .status("booking")
                    .message("unsuccess : " + e.getMessage())
                    .build();
        }
    }

    //Done
    @Override
    @Transactional
    public BookingResponse cancelBooking(String bookingId) {
        try {
            Optional<BookingOrderEntity> bookingOpt = bookingRepository.findById(bookingId);
            if (bookingOpt.isEmpty()) {
                return BookingResponse.builder()
                        .status("booking-cancel")
                        .message("no record exists for booking id : " + bookingId)
                        .build();
            }
            BookingOrderEntity booking = bookingOpt.get();
            bookingRepository.deleteById(bookingId);

            // Publish booking cancelled notification
            Optional<UserEntity> userOpt = userRepository.findById(booking.getUserId());
            EventEntity event = eventRepository.findEventEntityByEventIdContainingIgnoreCase(booking.getEventId());
            if (userOpt.isPresent() && event != null) {
                notificationProducer.publishBookingCancelled(BookingCancelledEvent.builder()
                        .email(userOpt.get().getEmail())
                        .username(userOpt.get().getUsername())
                        .eventName(event.getName())
                        .bookingId(bookingId)
                        .build());
            }

            return BookingResponse.builder()
                    .status("booking-cancel")
                    .message("success")
                    .build();
        } catch (Exception e) {
            return BookingResponse.builder()
                    .status("booking-cancel")
                    .message("unsuccess")
                    .build();
        }
    }

    //Done
    @Override
    public BookingResponse getUserBookingDetails(Long userId) {
        List<BookingOrderEntity> allByUserId = bookingRepository.findAllByUserId(userId);
        if (allByUserId.isEmpty()) {
            return BookingResponse.builder()
                    .status("booking-details")
                    .message("no record exists for user id : " + userId)
                    .build();
        } else {
            List<BookingOrder> collect = allByUserId.stream().map(this::toBookingOrder).collect(Collectors.toList());
            return BookingResponse.builder()
                    .status("booking-details")
                    .message("record found for user id : " + userId)
                    .orderList((ArrayList<BookingOrder>) collect)
                    .build();
        }
    }

    private BookingOrder toBookingOrder(BookingOrderEntity entity) {
        return BookingOrder.builder()
                .bookingId(entity.getBookingId())
                .eventId(entity.getEventId())
                .ticketNumber(entity.getTicketNumber())
                .orderDate(entity.getOrderedDate())
                .userId(entity.getUserId())
                .orderStatus(entity.getOrderStatus())
                .build();
    }

    private String generateTicketNumber() {
        LocalDateTime dateTime = LocalDateTime.now();

        String date = dateTime.format(DateTimeFormatter.ofPattern("ddMMyyyy"));
        String time = dateTime.format(DateTimeFormatter.ofPattern("ssmmHH"));

        long todayCount = bookingRepository.countByOrderedDateBetween(
                dateTime.toLocalDate().atStartOfDay(),
                dateTime.toLocalDate().plusDays(1).atStartOfDay()
        );
        String countSeq = String.format("%05d", todayCount);
        return "TKT" + countSeq + time + date;
    }

    private String generateBookingId() {
        String prefix = "B";
        String uniquePart = UUID.randomUUID().toString()
                .replace("-", "")
                .substring(0, 9)
                .toUpperCase();
        return prefix + uniquePart;
    }
}
