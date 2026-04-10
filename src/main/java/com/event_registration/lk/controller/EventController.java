package com.event_registration.lk.controller;

import com.event_registration.lk.dto.Event;
import com.event_registration.lk.dto.response.EventResponse;
import com.event_registration.lk.service.EventService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import jakarta.validation.Valid;
//Done
@RestController
@RequestMapping("/event")
@CrossOrigin
@Slf4j
public class EventController {

    EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    //Done
    @PostMapping
    public ResponseEntity<EventResponse> addEvent(@Valid @RequestBody Event event) {
        log.info("Received event: name={}, description={}, location={}, dates={}", 
                event.getName(), event.getDescription(), event.getLocation(), event.getDates());
        EventResponse response = eventService.addEvent(event);
        if ("success".equals(response.getMessage())) {
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        }
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @DeleteMapping("/{eventId}")
    public ResponseEntity<EventResponse> deleteEvent(@PathVariable String eventId) {
        EventResponse response = eventService.removeEvent(eventId);
        if ("success".equals(response.getMessage())) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @GetMapping
    public ResponseEntity<EventResponse> getAllEvents() {
        return ResponseEntity.ok(eventService.getAllEvents());
    }

    @PutMapping
    public ResponseEntity<EventResponse> updateEvent(@Valid @RequestBody Event event) {
        EventResponse response = eventService.updateEvent(event);
        if ("success".equals(response.getMessage())) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

}
