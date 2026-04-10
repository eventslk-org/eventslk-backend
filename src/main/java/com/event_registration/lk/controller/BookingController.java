package com.event_registration.lk.controller;

import com.event_registration.lk.dto.request.BookingRequest;
import com.event_registration.lk.dto.response.BookingResponse;
import com.event_registration.lk.service.BookingService;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import jakarta.validation.Valid;
//Done
@RestController
@RequestMapping("/book")
@CrossOrigin
public class BookingController {

    BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    public ResponseEntity<BookingResponse> bookEvent(@Valid @RequestBody BookingRequest bookingRequest){
        BookingResponse response = bookingService.bookEvent(bookingRequest);
        if (response.getMessage() != null && response.getMessage().startsWith("success")) {
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
    
    @GetMapping("/user/{userId}")
    public ResponseEntity<BookingResponse> getUserBookingDetails(@PathVariable Long userId){
        BookingResponse response = bookingService.getUserBookingDetails(userId);
        if (response.getMessage() != null && response.getMessage().startsWith("record found")) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }
    
    @DeleteMapping("/{bookingId}")
    public ResponseEntity<BookingResponse> cancelBooking(@PathVariable String bookingId){
        BookingResponse response = bookingService.cancelBooking(bookingId);
        if ("success".equals(response.getMessage())) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }
}
