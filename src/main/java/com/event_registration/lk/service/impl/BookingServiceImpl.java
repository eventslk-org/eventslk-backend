package com.event_registration.lk.service.impl;

import com.event_registration.lk.dto.BookingOrder;
import com.event_registration.lk.dto.request.BookingRequest;
import com.event_registration.lk.dto.response.BookingResponse;
import com.event_registration.lk.entity.BookingOrderEntity;
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

    public BookingServiceImpl(BookingRepository bookingRepository, EventRepository eventRepository, UserRepository userRepository, ObjectMapper objectMapper) {
        this.bookingRepository = bookingRepository;
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
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

            ArrayList<BookingOrder> bookingOrderList = new ArrayList<>();
            bookingOrderList.add(order);
            return BookingResponse.builder()
                    .status("booking")
                    .message("success")
                    .orderList(bookingOrderList)
                    .build();
        } catch (Exception e) {
            log.info("error in booking event : "+e.getMessage());
            return BookingResponse.builder()
                    .status("booking")
                    .message("unsuccess : "+e.getMessage())
                    .build();
        }
    }
    //Done
    @Override
    @Transactional
    public BookingResponse cancelBooking(String bookingId) {
        try{
            if(!bookingRepository.existsByBookingId(bookingId)){
                return BookingResponse.builder()
                        .status("booking-cancel")
                        .message("no record exists for booking id : " + bookingId + "")
                        .build();
            }
            bookingRepository.deleteById(bookingId);
            return BookingResponse.builder()
                    .status("booking-cancel")
                    .message("success")
                    .build();
        }catch (Exception e){
            return BookingResponse.builder()
                    .status("booking-cancel")
                    //.message("error occurred : "+e.getMessage())
                    .message("unsuccess")
                    .build();
        }
    }
    //Done
    @Override
    public BookingResponse getUserBookingDetails(Long userId) {
        List<BookingOrderEntity> allByUserId = bookingRepository.findAllByUserId(userId);
        if (allByUserId.isEmpty()){
            return BookingResponse.builder()
                    .status("booking-details")
                    .message("no record exists for user id : " + userId)
                    .build();
        }else {
            List<BookingOrder> collect = allByUserId.stream().map(this::toBookingOrder).collect(Collectors.toList());
            return BookingResponse.builder()
                    .status("booking-details")
                    .message("record found for user id : " + userId)
                    .orderList((ArrayList<BookingOrder>) collect)
                    .build();
        }
    }

    //convert BookingOrder entity to dto
    private BookingOrder toBookingOrder(BookingOrderEntity entity){
        return BookingOrder.builder()
                .bookingId(entity.getBookingId())
                .eventId(entity.getEventId())
                .ticketNumber(entity.getTicketNumber())
                .orderDate(entity.getOrderedDate())
                .userId(entity.getUserId())
                .orderStatus(entity.getOrderStatus())
                .build();
    }
    //Automatically generate ticket number
    private String generateTicketNumber(){
        LocalDateTime dateTime = LocalDateTime.now();

        String date = dateTime.format(DateTimeFormatter.ofPattern("ddMMyyyy"));
        String time = dateTime.format(DateTimeFormatter.ofPattern("ssmmHH"));

        long todayCount = bookingRepository.countByOrderedDateBetween(
                dateTime.toLocalDate().atStartOfDay(),
                dateTime.toLocalDate().plusDays(1).atStartOfDay()
        );
        String countSeq = String.format("%05d", todayCount);
        return "TKT"+countSeq+time+date;
    }
    //Automatically generate booking id
    private String generateBookingId() {
        String prefix = "B";
        String uniquePart = UUID.randomUUID().toString()
                .replace("-", "")
                .substring(0, 9)
                .toUpperCase();
        return prefix + uniquePart;
    }
}
